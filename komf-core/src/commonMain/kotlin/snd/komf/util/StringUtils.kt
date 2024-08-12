package snd.komf.util

// TODO better way?
private val fullWidthMap = mapOf(
    '　' to ' ', '！' to '!', '＂' to '"', '＃' to '#', '＄' to '$', '％' to '%', '＆' to '&',
    '＇' to '\'', '（' to '(', '）' to ')', '＊' to '*', '＋' to '+', '，' to ',', '－' to '-',
    '．' to '.', '／' to '/',
    '０' to '0', '１' to '1', '２' to '2', '３' to '3', '４' to '4', '５' to '5', '６' to '6',
    '７' to '7', '８' to '8', '９' to '9',
    '：' to ':', '；' to ';', '＜' to '<', '＝' to '=', '＞' to '>', '？' to '?', '＠' to '@',
    'Ａ' to 'A', 'Ｂ' to 'B', 'Ｃ' to 'C', 'Ｄ' to 'D', 'Ｅ' to 'E', 'Ｆ' to 'F', 'Ｇ' to 'G',
    'Ｈ' to 'H', 'Ｉ' to 'I', 'Ｊ' to 'J', 'Ｋ' to 'K', 'Ｌ' to 'L', 'Ｍ' to 'M', 'Ｎ' to 'N',
    'Ｏ' to 'O', 'Ｐ' to 'P', 'Ｑ' to 'Q', 'Ｒ' to 'R', 'Ｓ' to 'S', 'Ｔ' to 'T', 'Ｕ' to 'U',
    'Ｖ' to 'V', 'Ｗ' to 'W', 'Ｘ' to 'X', 'Ｙ' to 'Y', 'Ｚ' to 'Z',
    '［' to '[', '＼' to '\\',
    '］' to ']', '＾' to '^', '＿' to '_', '｀' to '`',
    'ａ' to 'a', 'ｂ' to 'b', 'ｃ' to 'c', 'ｄ' to 'd', 'ｅ' to 'e', 'ｆ' to 'f', 'ｇ' to 'g',
    'ｈ' to 'h', 'ｉ' to 'i', 'ｊ' to 'j', 'ｋ' to 'k', 'ｌ' to 'l', 'ｍ' to 'm', 'ｎ' to 'n',
    'ｏ' to 'o', 'ｐ' to 'p', 'ｑ' to 'q', 'ｒ' to 'r', 'ｓ' to 's', 'ｔ' to 't', 'ｕ' to 'u',
    'ｖ' to 'v', 'ｗ' to 'w', 'ｘ' to 'x', 'ｙ' to 'y', 'ｚ' to 'z',
    '｛' to '{', '｜' to '|', '｝' to '}'
)

private val fullwidthRegex = "[\uff01-\uff5e]".toRegex()

fun replaceFullwidthChars(input: String) = input.replace(fullwidthRegex) { match ->
    fullWidthMap[match.value.first()]?.toString() ?: match.value
}

expect fun stripAccents(input: String): String
