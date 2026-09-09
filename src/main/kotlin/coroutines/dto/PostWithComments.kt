package ru.netology.coroutines.dto

import coroutines.dto.Post

data class PostWithComments(
    val post: Post,
    val comments: List<Comment>,
)
