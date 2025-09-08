package com.ririv.quickoutline.view.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import com.ririv.quickoutline.generated.resources.Res
import com.ririv.quickoutline.generated.resources.actual_size
import com.ririv.quickoutline.generated.resources.bookmark
import com.ririv.quickoutline.generated.resources.delete
import com.ririv.quickoutline.generated.resources.fit_to_height
import com.ririv.quickoutline.generated.resources.fit_to_width
import com.ririv.quickoutline.generated.resources.github
import com.ririv.quickoutline.generated.resources.help
import com.ririv.quickoutline.generated.resources.open
import com.ririv.quickoutline.generated.resources.setting
import com.ririv.quickoutline.generated.resources.text_edit
import com.ririv.quickoutline.generated.resources.toc
import com.ririv.quickoutline.generated.resources.tree_diagram
import com.ririv.quickoutline.generated.resources.xiaohongshu
import com.ririv.quickoutline.generated.resources.`删除`
import com.ririv.quickoutline.generated.resources.`特色_风景`
import com.ririv.quickoutline.generated.resources.`页码_单路径`

/**
 * 统一的应用内图标枚举, 后续新增图标放这里。
 * 命名规则: 使用 PascalCase, 与资源文件名(去掉后缀)语义一致。
 */
sealed interface AppIcon {
    // 社交 / 品牌
    data object Github: AppIcon
    data object Xiaohongshu: AppIcon

    // 主侧栏功能
    data object Bookmark: AppIcon
    data object PageLabelSingle: AppIcon // 页码-单路径.svg
    data object Toc: AppIcon
    data object FeatureLandscape: AppIcon // 特色-风景.svg
    data object Setting: AppIcon
    data object Help: AppIcon

    // 书签底部 / 视图控制
    data object FitToHeight: AppIcon
    data object FitToWidth: AppIcon
    data object ActualSize: AppIcon
    data object TreeDiagram: AppIcon
    data object TextEdit: AppIcon
    data object Open: AppIcon
    data object Delete: AppIcon // delete.svg
    data object DeleteCn: AppIcon // 删除.svg (中文版本)
}

@Composable
fun AppIcon(
    icon: AppIcon,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val drawableRes = remember(icon) {
        when(icon) {
            AppIcon.Github -> Res.drawable.github
            AppIcon.Xiaohongshu -> Res.drawable.xiaohongshu
            AppIcon.Bookmark -> Res.drawable.bookmark
            AppIcon.PageLabelSingle -> Res.drawable.`页码_单路径`
            AppIcon.Toc -> Res.drawable.toc
            AppIcon.FeatureLandscape -> Res.drawable.`特色_风景`
            AppIcon.Setting -> Res.drawable.setting
            AppIcon.Help -> Res.drawable.help
            AppIcon.FitToHeight -> Res.drawable.fit_to_height
            AppIcon.FitToWidth -> Res.drawable.fit_to_width
            AppIcon.ActualSize -> Res.drawable.actual_size
            AppIcon.TreeDiagram -> Res.drawable.tree_diagram
            AppIcon.TextEdit -> Res.drawable.text_edit
            AppIcon.Open -> Res.drawable.`open`
            AppIcon.Delete -> Res.drawable.delete
            AppIcon.DeleteCn -> Res.drawable.`删除`
        }
    }
    Icon(painter = painterResource(drawableRes), contentDescription = contentDescription, modifier = modifier, tint = tint)
}
