package com.example.musikku

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSearch() {
        org.schabi.newpipe.extractor.NewPipe.init(com.example.musikku.data.remote.youtube.NewPipeDownloader())
        val queryHandler = org.schabi.newpipe.extractor.ServiceList.YouTube.searchQHFactory.fromQuery("sza snooze")
        val searchInfo = org.schabi.newpipe.extractor.search.SearchInfo.getInfo(
            org.schabi.newpipe.extractor.ServiceList.YouTube,
            queryHandler
        )
        println("Search results: " + searchInfo.relatedItems.size)
        for (item in searchInfo.relatedItems.take(5)) {
            if (item is org.schabi.newpipe.extractor.stream.StreamInfoItem) {
                println("Found: ${item.name}, uploader: ${item.uploaderName}, url: ${item.url}")
            }
        }
        org.junit.Assert.assertTrue(searchInfo.relatedItems.isNotEmpty())
        val firstItem = searchInfo.relatedItems.first() as org.schabi.newpipe.extractor.stream.StreamInfoItem
        println("Testing extraction for: ${firstItem.url}")
        val streamInfo = org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
            org.schabi.newpipe.extractor.ServiceList.YouTube,
            firstItem.url
        )
        val bestAudio = streamInfo.audioStreams.firstOrNull { it.itag == 140 } ?: streamInfo.audioStreams.first()
        org.junit.Assert.assertNotNull(bestAudio.content)
        org.junit.Assert.assertTrue(bestAudio.content.contains("googlevideo.com"))
    }
}