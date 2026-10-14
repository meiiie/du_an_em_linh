package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.tutor.application.service.SentenceBuffer;

class SentenceBufferTest {
    @Test void repeatedTerminalPunctuationNeverBecomesAnExtraSentence() {
        for(String text:List.of("Em đã thử chưa?! Em chọn cách nào?", "Em thử xem... Em chọn cách nào?")) {
            for(int split=0;split<=text.length();split++) {
                var sentences=new ArrayList<String>();var buffer=new SentenceBuffer(sentences::add);
                buffer.add(text.substring(0,split));buffer.add(text.substring(split));buffer.finish();
                assertEquals(2,sentences.size(),"Vị trí "+split+": "+sentences);
                assertTrue(sentences.stream().allMatch(s -> s.codePoints().anyMatch(Character::isLetter)));
            }
            var sentences=new ArrayList<String>();var buffer=new SentenceBuffer(sentences::add);
            text.codePoints().forEach(c -> buffer.add(Character.toString(c)));buffer.finish();
            assertEquals(2,sentences.size());
        }
    }
    @Test void terminalPunctuationDoesNotWaitForAnotherChunkOrEof() {
        for(String text:List.of("Em đã thử gì?", "Em tự kiểm tra nhé!", "Em xem dòng này.")) {
            var sentences=new ArrayList<String>();var buffer=new SentenceBuffer(sentences::add);
            for(int i=0;i<text.length();i++)buffer.add(text.substring(i,i+1));
            assertEquals(List.of(text),sentences);
            buffer.finish();assertEquals(1,sentences.size());
        }
    }
    @Test void escapedDollarCannotCloseMathAndDecimalCannotEndASentence() {
        var sentences=new ArrayList<String>();var buffer=new SentenceBuffer(sentences::add);
        buffer.add("Em viết $a\\$? ");assertTrue(sentences.isEmpty());
        buffer.add("b$ và 1.");assertTrue(sentences.isEmpty());
        buffer.add("5 thế nào?");assertEquals(List.of("Em viết $a\\$? b$ và 1.5 thế nào?"),sentences);
    }
    @Test void everyChunkBoundaryPreservesVietnameseAndMath() {
        String text="Em xét $x=1.5$ chưa? Em xem \\(a.b\\) và $$x. y$$ rồi thử nhé. \\[a!b\\] có nghĩa gì?";
        for(int split=0;split<=text.length();split++) {
            var sentences=new ArrayList<String>();var buffer=new SentenceBuffer(sentences::add);
            buffer.add(text.substring(0,split));buffer.add(text.substring(split));buffer.finish();
            assertEquals(3,sentences.size(),"Vị trí "+split);assertEquals(text,String.join("",sentences));
        }
    }
    @Test void doesNotReleaseIncompleteMathAndRejectsItAtEof() {
        var sentences=new ArrayList<String>();var buffer=new SentenceBuffer(sentences::add);
        buffer.add("Em viết $x. ");assertTrue(sentences.isEmpty());assertThrows(IllegalStateException.class,buffer::finish);assertTrue(sentences.isEmpty());
    }
    @Test void streamsCompletedSentenceBeforeModelFinishes() {
        var sentences=new ArrayList<String>();var buffer=new SentenceBuffer(sentences::add);
        buffer.add("Em đã thử gì? ");assertEquals(List.of("Em đã thử gì? "),sentences);
        buffer.add("Em xem tiếp nhé");assertEquals(1,sentences.size());buffer.finish();assertEquals(2,sentences.size());
    }
}
