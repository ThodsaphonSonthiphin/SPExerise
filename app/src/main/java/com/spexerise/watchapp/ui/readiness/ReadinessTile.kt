package com.spexerise.watchapp.ui.readiness

import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.LayoutElementBuilders
import com.google.common.util.concurrent.ListenableFuture
import com.spexerise.watchapp.data.db.AppDatabase
import com.spexerise.watchapp.domain.readiness.ReadinessCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

class ReadinessTile : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CoroutineScope(Dispatchers.IO).future {
            val db = AppDatabase.getInstance(this@ReadinessTile)
            val snapshot = db.readinessDao().getLatest()
            val score = snapshot?.score ?: 0
            val label = ReadinessCalculator.label(score)

            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setTimeline(
                    TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(
                            TimelineBuilders.TimelineEntry.Builder()
                                .setLayout(
                                    LayoutElementBuilders.Layout.Builder()
                                        .setRoot(
                                            LayoutElementBuilders.Column.Builder()
                                                .addContent(
                                                    LayoutElementBuilders.Text.Builder()
                                                        .setText("READINESS $score")
                                                        .build()
                                                )
                                                .addContent(
                                                    LayoutElementBuilders.Text.Builder()
                                                        .setText(label)
                                                        .build()
                                                )
                                                .build()
                                        ).build()
                                ).build()
                        ).build()
                ).build()
        }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CoroutineScope(Dispatchers.IO).future {
            ResourceBuilders.Resources.Builder().setVersion("1").build()
        }
}
