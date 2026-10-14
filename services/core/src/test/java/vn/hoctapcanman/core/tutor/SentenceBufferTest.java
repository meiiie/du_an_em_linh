package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.tutor.application.service.SentenceBuffer;

class SentenceBufferTest {
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
