package com.despicable.ladybugos.model


data class LabelData(
    val color: String,
    val createdTimestampUsec: Long,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isTrashed: Boolean,
    val labels: List<Label>,
    val textContent: String,
    val textContentHtml: String,
    val title: String,
    val userEditedTimestampUsec: Long
)

data class Label(
    val name: String
)

data class ListData(
    val color: String,
    val createdTimestampUsec: Long,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isTrashed: Boolean,
    val listContent: List<Content>,
    val title: String,
    val userEditedTimestampUsec: Long
)

data class Content(
    val isChecked: Boolean,
    val text: String,
    val textHtml: String
)

data class TextContent(
    val color: String,
    val createdTimestampUsec: Long,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isTrashed: Boolean,
    val textContent: String,
    val textContentHtml: String,
    val title: String,
    val userEditedTimestampUsec: Long
)


data class TitleContent(     // No textContentHtml if textContent is ""
    val color: String,
    val createdTimestampUsec: Long,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isTrashed: Boolean,
    val textContent: String,
    val title: String,
    val userEditedTimestampUsec: Long
)


data class WebLinks(
    val annotations: List<Annotation>,
    val color: String,
    val createdTimestampUsec: Long,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isTrashed: Boolean,
    val textContent: String,
    val textContentHtml: String,
    val title: String,
    val userEditedTimestampUsec: Long
)

data class Annotation(
    val description: String,
    val source: String,
    val title: String,
    val url: String
)


