package com.ririv.quickoutline.api.model;

import com.ririv.quickoutline.model.Bookmark;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookmarkDto {
    private String id;
    private String title;
    private Integer page; // Maps to 'pageNum' in domain, 'page' in frontend
    private int level;
    private List<BookmarkDto> children = new ArrayList<>();

    public static BookmarkDto fromDomain(Bookmark domain) {
        if (domain == null) return null;
        
        BookmarkDto dto = new BookmarkDto();
        dto.id = domain.getId();
        dto.title = domain.getTitle();
        dto.page = domain.getPageNum().orElse(0);
        dto.level = domain.getLevel();
        
        if (domain.getChildren() != null) {
            dto.children = domain.getChildren().stream()
                .map(BookmarkDto::fromDomain)
                .collect(Collectors.toList());
        }
        return dto;
    }

    public Bookmark toDomain() {
        Bookmark bookmark = new Bookmark(title, page, level);
        // Note: ID is regenerated, but for serialization to text, ID is irrelevant.
        
        if (children != null) {
            for (BookmarkDto childDto : children) {
                bookmark.addChild(childDto.toDomain());
            }
        }
        return bookmark;
    }
}
