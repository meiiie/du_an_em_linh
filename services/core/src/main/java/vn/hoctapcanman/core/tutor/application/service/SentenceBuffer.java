package vn.hoctapcanman.core.tutor.application.service;

import java.util.function.Consumer;

/** Ranh giới câu chỉ ở ngoài delimiter LaTeX; giữ nguyên khoảng trắng qua chunk. */
public final class SentenceBuffer {
    private final StringBuilder pending = new StringBuilder();
    private final Consumer<String> sentences;
    public SentenceBuffer(Consumer<String> sentences) { this.sentences = sentences; }
    public void add(String chunk) {
        pending.append(chunk);
        if (pending.length() > 12000) throw new IllegalStateException("OUTPUT_LIMIT");
        drain(false);
    }
    public void finish() { drain(true); }
    private void drain(boolean finished) {
        while (!pending.isEmpty()) {
            String text = pending.toString();
            String math = "";
            int end = -1;
            for (int i = 0; i < text.length(); i++) {
                if (!math.isEmpty()) {
                    if (text.startsWith(math, i) && !escaped(text,i)) { i += math.length()-1; math = ""; }
                    continue;
                }
                if (escaped(text,i)) continue;
                if (text.startsWith("$$", i)) { math="$$"; i++; continue; }
                if (text.charAt(i)=='$') { math="$"; continue; }
                if (text.startsWith("\\(", i)) { math="\\)"; i++; continue; }
                if (text.startsWith("\\[", i)) { math="\\]"; i++; continue; }
                char c = text.charAt(i);
                // ?/! kết thúc câu ngay, kể cả token cuối chưa có khoảng trắng.
                // Dấu chấm sau chữ cũng vậy; sau số phải chờ để không cắt 1.5.
                boolean terminal=c=='?' || c=='!' || c=='.' &&
                    (i+1<text.length() ? Character.isWhitespace(text.charAt(i+1)) :
                        finished || i>0 && !Character.isDigit(text.charAt(i-1)));
                if (terminal) {
                    end=i+1;
                    while (end<text.length() && ".?!".indexOf(text.charAt(end))>=0) end++;
                    while (end<text.length() && Character.isWhitespace(text.charAt(end))) end++;
                    break;
                }
            }
            if (end<0) {
                if (!finished) return;
                if (!math.isEmpty()) throw new IllegalStateException("UNCLOSED_MATH");
                end=text.length();
            }
            String sentence=text.substring(0,end);
            pending.delete(0,end);
            // Punctuation có thể tới sau khi câu vừa phát; không tính nó là câu mới.
            if (sentence.codePoints().anyMatch(c -> !Character.isWhitespace(c) && ".?!".indexOf(c)<0)) sentences.accept(sentence);
        }
    }
    private static boolean escaped(String text,int at) {
        int slashes=0;
        for(int i=at-1;i>=0 && text.charAt(i)=='\\';i--)slashes++;
        return slashes%2!=0;
    }
}
