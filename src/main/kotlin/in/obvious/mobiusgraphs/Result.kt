package `in`.obvious.mobiusgraphs

import java.awt.image.BufferedImage

sealed class Result

data class Success(val image: BufferedImage) : Result()

data class Failure(val cause: Throwable) : Result()