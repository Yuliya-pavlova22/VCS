package svcs2
import java.io.File
import java.security.MessageDigest

// Скомпилировать исходник в jar-файл
//      kotlinc Main.kt -include-runtime -d main.jar

// Запуск программы
//      java -jar main.jar --help
//  всё, что идёт после .jar - аргументы для main-функции

// одной строкой:
//      kotlinc Main.kt -include-runtime -d main.jar && java -jar main.jar --help
// функция получения хеша из массива ByteArray
fun hash(byteHash: ByteArray): String {
    val hash = MessageDigest.getInstance("SHA-256")
    val digest = hash.digest(byteHash)
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}

fun main(args: Array<String>) {
    var myVcs = File("vcs")
    myVcs.mkdir()
    var configFile = File("vcs/config.txt")
    configFile.createNewFile()
    var commits = File("vcs/commits")
    commits.mkdir()
    var logFile = File("vcs/log.txt")
    logFile.createNewFile()

    var help = mapOf<String, String>(
        "config" to "Get and set a username.",
        "add" to "Add a file to the index.",
        "log" to "Show commit logs.",
        "commit" to "Save changes.",
        "checkout" to "Restore a file."
    )
    if (args.isEmpty()) {
        printHelp(help)
        return
    }
    when (args.first()) {
        "config" -> println(config(args, configFile))
        "add" -> {
            var indexFile = File("vcs/index.txt")
            indexFile.createNewFile()
            println(add(args, indexFile))
//  при вызове add перезаписываем файлик с хешом всех имеющихся файлов
            var hashFile = File("hash.txt")
            hashFile.writeText(getHeshIndex()+1)
        }
        "log" -> {
            if (logFile.readText()== "") println("No commits yet.")
            else print(logFile.readText())
        }
        "commit" -> {
            if (args.size > 1) {
//проверяем изменились ли файлы
                var hashFile2 = File("hash.txt")
                var oldHesh = hashFile2.readText()
                var aktualHesh = getHeshIndex()
                if (oldHesh == aktualHesh) println("Nothing to commit.")
                else {
      // заполняем log файл и далее создаем папку со значением Хеша
                    var oldLog = logFile.readText()
                    logFile.writeText("commit $aktualHesh\n" +
                            "Author: ${configFile.readText()}" +
                            "\n" +
                            "${args[1]}")
                    if (oldLog != "") logFile.appendText("\n$oldLog")

                    var hashDir = File("vcs/commits/$aktualHesh")
                    hashDir.mkdir()
// копируем все файлы из index.txt  в новую папку с хешом
                var indexFile2 = File("vcs/index.txt")
                indexFile2.forEachLine {
                    var file = File(it)
                    var commitFileNew = File("vcs/commits/$aktualHesh/${file.name}")
                    file.copyTo(commitFileNew)
                }
                    println("Changes are committed.")
                    // после внесения коммита обновляем хеш
                    hashFile2.writeText(aktualHesh)
                }
            } else println("Message was not passed.")
        }
        "checkout" -> {
            if (args.size > 1) {
                var fountHash = File("vcs/commits/${args[1]}")

                if (fountHash.exists()) {
                    // проходимся по нужному каталогу и находим все файлы
                    fountHash.walk().forEach { f ->
                    // замена файлов
                    var oldFile = File("${f.name}")
                        if (oldFile.exists()) {
                          //  println("файл найден")
                            var str = f.readText()
                        oldFile.writeText(str)
                        }
                    }
                    println("Switched to commit ${args[1]}.")
                }
                else println("Commit does not exist.")


            } else println("Commit id was not passed.")
        }
        "--help" ->  printHelp(help)
        else -> println("'${args.first()}' is not a SVCS command.")
    }
}
// добавление файлов в индекс
fun add(args: Array<String>, indexFile: File) : String {
    if (args.size > 1) {
        var newFile = File("${args[1]}")

        if (!newFile.exists()) {
            return "Can't find '${args[1]}'."
        }
        indexFile.appendText("${args[1]}\n")

        return "The file '${args[1]}' is tracked. "
    } else
        if (indexFile.readText() == "")
            return  "Add a file to the index."
        else {
            return "Tracked files:\n" +
            "${indexFile.readText()}"
        }
}
// обработка config
fun config(myArgs: Array<String>, file: File): String {
    return if (myArgs.size > 1) {
        file.writeText(myArgs[1])
        "The username is ${file.readText()}."
    } else
        if (file.readText() == "")
            "Please, tell me who you are."
        else
            "The username is ${file.readText()}."
}
// распечатка Help
fun printHelp(listMap : Map<String, String>) {
    println("These are SVCS commands:")
    for (element in listMap) {
        println("${element.key.padEnd(11)}${element.value}")
    }
}
//функция получения хеша из файлов с Index.txt
fun getHeshIndex() : String {
    var myFileIndex = File("vcs/index.txt")
    var byteStr: ByteArray
    var str = ""
    myFileIndex.forEachLine {
        var fail = File(it)
        str += fail.readText()
    }
    byteStr = str.toByteArray()
    return hash(byteStr)
}

