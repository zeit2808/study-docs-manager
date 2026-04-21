package com.studydocs.manager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    private int fullTextMaxLength = 10000;
    private boolean highlightEnabled = false;
    private String highlightPreTag = "<em class=\"highlight\">";
    private String highlightPostTag = "</em>";
    private boolean fuzzyEnabled = false;
    private String fuzzyFuzziness = "AUTO";
    private int maxPageSize = 50;

    public int getFullTextMaxLength() {
        return fullTextMaxLength;
    }

    public void setFullTextMaxLength(int fullTextMaxLength) {
        this.fullTextMaxLength = fullTextMaxLength;
    }

    public boolean isHighlightEnabled() {
        return highlightEnabled;
    }

    public void setHighlightEnabled(boolean highlightEnabled) {
        this.highlightEnabled = highlightEnabled;
    }

    public String getHighlightPreTag() {
        return highlightPreTag;
    }

    public void setHighlightPreTag(String highlightPreTag) {
        this.highlightPreTag = highlightPreTag;
    }

    public String getHighlightPostTag() {
        return highlightPostTag;
    }

    public void setHighlightPostTag(String highlightPostTag) {
        this.highlightPostTag = highlightPostTag;
    }

    public boolean isFuzzyEnabled() {
        return fuzzyEnabled;
    }

    public void setFuzzyEnabled(boolean fuzzyEnabled) {
        this.fuzzyEnabled = fuzzyEnabled;
    }

    public String getFuzzyFuzziness() {
        return fuzzyFuzziness;
    }

    public void setFuzzyFuzziness(String fuzzyFuzziness) {
        this.fuzzyFuzziness = fuzzyFuzziness;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}
