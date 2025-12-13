use std::path::PathBuf;
use std::sync::Mutex;
use std::thread;
use tiny_http::{Server, Response, Header, Method};
use std::fs::File;
use tauri::{AppHandle, Manager, Runtime}; // Import Manager and Runtime trait

// Define a struct to hold the server state (port)
pub struct LocalServerState {
    pub port: Mutex<Option<u16>>,
    pub workspace: Mutex<PathBuf>,
}

impl LocalServerState {
    pub fn new() -> Self {
        Self {
            port: Mutex::new(None),
            workspace: Mutex::new(PathBuf::new()),
        }
    }
}

pub fn start_server<R: Runtime>(app_handle: AppHandle<R>, workspace_path: PathBuf) {
    let state = app_handle.state::<LocalServerState>();
    
    // Store workspace path
    *state.workspace.lock().unwrap() = workspace_path.clone();

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

            let file_path = workspace_path.join(&clean_path);

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
                } else if file_path.is_dir() {
                    // Generate directory listing
                    let mut html = String::from("<html><head><title>Directory Listing</title></head><body>");
                    html.push_str(&format!("<h1>Index of /{}</h1><ul>", clean_path));
                    
                    // Add parent directory link if not root
                    if !clean_path.is_empty() {
                         html.push_str("<li><a href=\"..\">.. (Parent Directory)</a></li>");
                    }

                    if let Ok(entries) = std::fs::read_dir(&file_path) {
                        for entry in entries.flatten() {
                            let filename = entry.file_name().to_string_lossy().to_string();
                            let is_dir = entry.file_type().map(|ft| ft.is_dir()).unwrap_or(false);
                            let display_name = if is_dir { format!("{}/", filename) } else { filename.clone() };
                            
                            // Construct relative link
                            // If current path is empty, link is just filename
                            // If clean_path ends with /, append filename
                            // Otherwise append /filename
                            // Ideally, tiny_http handles relative links in browser. 
                            // Simple href="filename" works if we are at the directory URL (ending in /).
                            // But if we are at "/fonts" (no slash), browser treats "arial.ttf" as sibling of "fonts".
                            // To be safe, we should ensure we redirect to trailing slash if missing, or use robust relative logic.
                            // For simplicity: href="./filename" works if URL ends in slash.
                            
                            html.push_str(&format!("<li><a href=\"{}\">{}</a></li>", filename, display_name));
                        }
                    }
                    html.push_str("</ul></body></html>");
                    
                    let mut response = Response::from_string(html);
                    let header = Header::from_bytes(&b"Content-Type"[..], &b"text/html"[..]).unwrap();
                    response.add_header(header);
                    let _ = request.respond(response);
                }
            } else {
                let _ = request.respond(Response::from_string("Not Found").with_status_code(404));
            }
        }
    });
}
