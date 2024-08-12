package snd.komf.comicinfo

import snd.komf.comicinfo.model.ComicInfo


interface ComicInfoWriter {

    fun removeComicInfo(localPath: String)

    fun writeMetadata(localPath: String, comicInfo: ComicInfo)

    class ComicInfoException(override val message: String) : RuntimeException(message)
}
