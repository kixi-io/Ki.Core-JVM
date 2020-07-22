package ki.test

import ki.err
import ki.log
import java.io.File
import java.net.URL

class Reflect {
    companion object {

        fun getTests(packageName: String): ArrayList<Test> {
            // Translate the package name into an absolute path
            var name = packageName
            if (!name.startsWith("/")) {
                name = "/$name"
            }
            name = name.replace('.', '/')

            // Get a File object for the package
            val url: URL = Test::class.java.getResource(name)
            val directory = File(url.getFile())
            val tests = ArrayList<Test>()

            if (directory.exists()) {
                // Get the list of the files contained in t  package
                directory.walk()
                    .filter { f -> f.isFile() && f.name.contains('$') == false && f.name.endsWith(".class") }
                    .forEach {
                        val fullyQualifiedClassName = packageName +
                                it.canonicalPath.removePrefix(directory.canonicalPath)
                                    .dropLast(6) // remove .class
                                    .replace('/', '.')
                        try {
                            if (fullyQualifiedClassName.endsWith("Test")) {
                                // TODO replace newInstance with current equivalent
                                val o = Class.forName(fullyQualifiedClassName)
                                    .getDeclaredConstructor().newInstance() as Test
                                tests.add(o)
                            }
                        } catch (cnfex: ClassNotFoundException) {
                            err(cnfex)
                        } catch (iex: InstantiationException) {
                            err(iex)
                        } catch (iaex: IllegalAccessException) {
                            err(iaex)
                        }
                    }
            }
            return tests
        }
    }
}