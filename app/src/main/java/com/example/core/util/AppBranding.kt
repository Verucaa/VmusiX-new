package com.example.core.util

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import java.io.InputStream

object AppBranding {
    const val APP_NAME = "Vmusix"
    const val ASSET_ICON_PATH = "file:///android_asset/app_icon.png"

    fun hasCustomAssetIcon(context: Context): Boolean {
        return try {
            context.assets.open("app_icon.png").use { true }
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
fun AppBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shapeRadius: Dp = 10.dp
) {
    val context = LocalContext.current
    val bitmap = remember(context) {
        runCatching {
            context.assets.open("app_icon.png").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(shapeRadius))
            .testTag("app_brand_logo"),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = AppBranding.APP_NAME,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(AppBranding.ASSET_ICON_PATH)
                    .crossfade(true)
                    .build(),
                contentDescription = AppBranding.APP_NAME,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        }
    }
}
