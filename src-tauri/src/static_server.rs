use std::path::PathBuf;
use std::sync::Mutex;
use std::thread;
use tiny_http::{Server, Response, Header, Method};
use std::fs::File;
use std::io::{Read, Seek, SeekFrom};
use tauri::{AppHandle, Manager, Runtime}; // Import Manager and Runtime trait

// Define a struct to hold the server state (port)
pub struct LocalServerState {
    pub port: Mutex<Option<u16>>,
    pub workspace: Mutex<PathBuf>,
    pub resources: Mutex<Option<PathBuf>>,
    pub current_pdf: Mutex<Option<PathBuf>>,
}

impl LocalServerState {
    pub fn new() -> Self {
        Self {
            port: Mutex::new(None),
            workspace: Mutex::new(PathBuf::new()),
            resources: Mutex::new(None),
            current_pdf: Mutex::new(None),
        }
    }
}

pub fn start_server<R: Runtime>(app_handle: AppHandle<R>, workspace_path: PathBuf, resources_path: Option<PathBuf>) {
    let state = app_handle.state::<LocalServerState>();
    
    // Store workspace and resources path
    *state.workspace.lock().unwrap() = workspace_path.clone();
    *state.resources.lock().unwrap() = resources_path.clone();

    // Spawn server in a separate thread
    thread::spawn(move || {
        // Bind to localhost on a random available port
        let server = match Server::http("127.0.0.1:0") {
            Ok(s) => s,
            Err(e) => {
                eprintln!("Failed to start local static server: {}", e);
                return;
            }
        };

        // Extract port from ListenAddr
        let port = match server.server_addr() {
            tiny_http::ListenAddr::IP(addr) => addr.port(),
            _ => {
                eprintln!("Server started on non-IP address, cannot retrieve port.");
                return;
            }
        };

        println!("Local Static Server listening on http://127.0.0.1:{}", port);

        // Update state with the port
        if let Some(state) = app_handle.try_state::<LocalServerState>() {
             *state.port.lock().unwrap() = Some(port);
        } else {
            eprintln!("Failed to access LocalServerState to set port.");
        }

        for request in server.incoming_requests() {
            // Only allow GET requests
            if request.method() != &Method::Get {
                 let _ = request.respond(Response::from_string("Method Not Allowed").with_status_code(405));
                 continue;
            }

            let url = request.url();
            
            // Intercept PDF request
            if url.starts_with("/pdf/current.pdf") {
                if let Some(state) = app_handle.try_state::<LocalServerState>() {
                    if let Some(pdf_path) = state.current_pdf.lock().unwrap().clone() {
                        serve_file_with_range(request, pdf_path);
                        continue;
                    }
                }
                let _ = request.respond(Response::from_string("Not Found (No PDF set)").with_status_code(404));
                continue;
            }

            // Basic path sanitization: prevent directory traversal
            if url.contains("..") {
                let _ = request.respond(Response::from_string("Forbidden").with_status_code(403));
                continue;
            }

            // Remove leading slash
            let relative_path = if url.len() > 1 { &url[1..] } else { "" };
            
            // Allow querying URL params (ignore them for file lookup)
            let path_end = relative_path.find('?').unwrap_or(relative_path.len());
            let clean_path = &relative_path[0..path_end];

            // Use URL decoding if needed (basic substitution here, expand if necessary)
            let clean_path = clean_path.replace("%20", " ");

            let mut file_path = workspace_path.join(&clean_path);
            let mut served_from_resources = false;

            // Strategy: Check workspace first, then resources fallback
            if !file_path.exists() {
                if let Some(res_dir) = &resources_path {
                    // Only fallback for specific asset directories to avoid serving unintended files
                    // Assuming structure: resources/libs/..., resources/fonts/...
                    // Match "libs", "libs/", "fonts", "fonts/"
                    if clean_path == "libs" || clean_path.starts_with("libs/") || 
                       clean_path == "fonts" || clean_path.starts_with("fonts/") {
                        
                        let potential_path = res_dir.join(&clean_path);
                        if potential_path.exists() {
                            file_path = potential_path;
                            served_from_resources = true;
                            println!("Serving from resources: {}", file_path.display()); // Log this
                        }
                    }
                }
            }

            if file_path.exists() {
                if file_path.is_file() {
                    if let Ok(file) = File::open(&file_path) {
                        let mut response = Response::from_file(file);
                        
                        // Add MIME type
                        let mime_type = match file_path.extension().and_then(|s| s.to_str()) {
                            Some("html") => "text/html",
                            Some("css") => "text/css",
                            Some("js") => "application/javascript",
                            Some("png") => "image/png",
                            Some("jpg") | Some("jpeg") => "image/jpeg",
                            Some("svg") => "image/svg+xml",
                            Some("pdf") => "application/pdf",
                            Some("woff") => "font/woff",
                            Some("woff2") => "font/woff2",
                            Some("ttf") => "font/ttf",
                            _ => "application/octet-stream",
                        };
                        
                        let header = Header::from_bytes(&b"Content-Type"[..], mime_type.as_bytes()).unwrap();
                        response.add_header(header);

                        // Add CORS headers just in case
                        let cors = Header::from_bytes(&b"Access-Control-Allow-Origin"[..], &b"*"[..]).unwrap();
                        response.add_header(cors);

                        let _ = request.respond(response);
                    } else {
                        let _ = request.respond(Response::from_string("Internal Server Error").with_status_code(500));
                    }
                } else if file_path.is_dir() { // Removed !served_from_resources check to allow listing virtual dirs
                    // Generate directory listing with merged view
                    let mut html = String::from("<html><head><title>Directory Listing</title><style>body{font-family:sans-serif;} table{border-collapse:collapse;width:100%;} td,th{padding:8px;text-align:left;border-bottom:1px solid #ddd;} .src{color:#888;font-size:0.8em;}</style></head><body>");
                    html.push_str(&format!("<h1>Index of /{}</h1><table><tr><th>Name</th><th>Source</th></tr>", clean_path));
                    
                    if !clean_path.is_empty() {
                         html.push_str("<tr><td><a href=\"..\">.. (Parent Directory)</a></td><td></td></tr>");
                    }

                    // Use a Set to track seen filenames to avoid duplicates
                    let mut seen_files = std::collections::HashSet::new();
                    let mut entries = Vec::new();

                    // 1. Scan Workspace (High Priority)
                    if let Ok(ws_entries) = std::fs::read_dir(&file_path) {
                        for entry in ws_entries.flatten() {
                            let filename = entry.file_name().to_string_lossy().to_string();
                            let is_dir = entry.file_type().map(|ft| ft.is_dir()).unwrap_or(false);
                            entries.push((filename.clone(), is_dir, "Workspace"));
                            seen_files.insert(filename);
                        }
                    }

                    // 2. Scan Resources (Fallback)
                    if let Some(res_dir) = &resources_path {
                        let res_target = res_dir.join(&clean_path);
                        if res_target.exists() && res_target.is_dir() {
                             if let Ok(res_entries) = std::fs::read_dir(&res_target) {
                                for entry in res_entries.flatten() {
                                    let filename = entry.file_name().to_string_lossy().to_string();
                                    if !seen_files.contains(&filename) {
                                        let is_dir = entry.file_type().map(|ft| ft.is_dir()).unwrap_or(false);
                                        entries.push((filename, is_dir, "Resources (Virtual)"));
                                    }
                                }
                             }
                        }
                    }

                    // Sort entries: Directories first, then alphabetical
                    entries.sort_by(|a, b| {
                        if a.1 != b.1 {
                            b.1.cmp(&a.1) // Directories first
                        } else {
                            a.0.cmp(&b.0)
                        }
                    });

                    for (filename, is_dir, source) in entries {
                        let display_name = if is_dir { format!("{}/", filename) } else { filename.clone() };
                        let link = if clean_path.is_empty() { filename.clone() } else { format!("{}/{}", clean_path, filename) }; 
                        // Note: link construction might need adjustment based on how browser handles relative links. 
                        // If we are at /libs/, href="paged.js" works. 
                        // If we are at /libs (no slash), href="paged.js" replaces "libs".
                        // Assuming tiny_http or browser handles the current URL context.
                        // Safest is relative just filename if we assume standard behavior.
                        
                        html.push_str(&format!("<tr><td><a href=\"{}\">{}</a></td><td class=\"src\">{}</td></tr>", filename, display_name, source));
                    }

                    html.push_str("</table></body></html>");
                    
                    let mut response = Response::from_string(html);
                    let header = Header::from_bytes(&b"Content-Type"[..], &b"text/html"[..]).unwrap();
                    response.add_header(header);
                    let _ = request.respond(response);
                } else {
                     // Directory listing disabled for resources or failed
                     let _ = request.respond(Response::from_string("Forbidden").with_status_code(403));
                }
            } else {
                let _ = request.respond(Response::from_string("Not Found").with_status_code(404));
            }
        }
    });
}

fn serve_file_with_range(request: tiny_http::Request, path: PathBuf) {
    let mut file = match File::open(&path) {
        Ok(f) => f,
        Err(_) => { let _ = request.respond(Response::from_string("Not Found").with_status_code(404)); return; }
    };
    let file_len = file.metadata().map(|m| m.len()).unwrap_or(0);

    let mut start = 0;
    let mut end = file_len - 1;
    let mut is_range = false;

    for header in request.headers() {
        if header.field.equiv("Range") {
            let val = header.value.as_str();
            if val.starts_with("bytes=") {
                let ranges: Vec<&str> = val["bytes=".len()..].split('-').collect();
                if let Ok(s) = ranges[0].parse::<u64>() {
                    start = s;
                }
                if ranges.len() > 1 && !ranges[1].is_empty() {
                    if let Ok(e) = ranges[1].parse::<u64>() {
                        end = e;
                    }
                }
                is_range = true;
            }
        }
    }
    
    if end >= file_len { end = if file_len > 0 { file_len - 1 } else { 0 }; }
    if start > end && file_len > 0 { 
        let _ = request.respond(Response::from_string("Range Not Satisfiable").with_status_code(416));
        return;
    }

    let len = end - start + 1;
    
    if let Err(_) = file.seek(SeekFrom::Start(start)) {
         let _ = request.respond(Response::from_string("Seek Failed").with_status_code(500));
         return;
    }

    let reader = Box::new(file.take(len)) as Box<dyn Read + Send + Sync + 'static>;

    let mut response = Response::new(
        if is_range { tiny_http::StatusCode(206) } else { tiny_http::StatusCode(200) },
        vec![
            Header::from_bytes(&b"Content-Type"[..], &b"application/pdf"[..]).unwrap(),
            Header::from_bytes(&b"Access-Control-Allow-Origin"[..], &b"*"[..]).unwrap(),
            Header::from_bytes(&b"Accept-Ranges"[..], &b"bytes"[..]).unwrap(),
            Header::from_bytes(&b"Content-Length"[..], format!("{}", len).as_bytes()).unwrap(),
        ],
        reader,
        Some(len as usize),
        None,
    );

    if is_range {
        response.add_header(Header::from_bytes(&b"Content-Range"[..], format!("bytes {}-{}/{}", start, end, file_len).as_bytes()).unwrap());
    }

    let _ = request.respond(response);
}
