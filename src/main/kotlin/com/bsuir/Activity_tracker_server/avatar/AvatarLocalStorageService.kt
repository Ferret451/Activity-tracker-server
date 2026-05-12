package com.bsuir.Activity_tracker_server.avatar

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.pathString

@Service
class AvatarLocalStorageService(
    private val props: AvatarProperties,
) {
    fun store(userId: UUID, file: MultipartFile): StoredAvatar {
        val contentType = file.contentType ?: throw IllegalArgumentException("contentType is required")
        val ext = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> throw IllegalArgumentException("Unsupported contentType: $contentType")
        }

        val userDir = Paths.get(props.baseDir).resolve(userId.toString())
        userDir.createDirectories()

        val filename = "${UUID.randomUUID()}.$ext"
        val target: Path = userDir.resolve(filename)
        file.inputStream.use { input ->
            Files.copy(input, target)
        }

        val publicBase = props.publicBaseUrl.trimEnd('/')
        val url = "$publicBase/${userId}/${filename}"
        return StoredAvatar(
            url = url,
            absolutePath = target.pathString,
        )
    }

    fun deleteByUrlIfLocal(url: String?) {
        if (url.isNullOrBlank()) return

        val publicBase = props.publicBaseUrl.trimEnd('/')
        if (!url.startsWith("$publicBase/")) return

        val relative = url.removePrefix("$publicBase/").trimStart('/')
        val path = Paths.get(props.baseDir).resolve(relative)
        if (path.exists()) {
            Files.delete(path)
        }
    }
}

data class StoredAvatar(
    val url: String,
    val absolutePath: String,
)

