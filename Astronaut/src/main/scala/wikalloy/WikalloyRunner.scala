package wikalloy

object WikalloyRunner {

  def main(args: Array[String]): Unit = {
    new WikalloyAnalyzer().run(args)
  }
}