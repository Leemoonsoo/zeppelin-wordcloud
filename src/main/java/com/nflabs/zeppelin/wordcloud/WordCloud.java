package com.nflabs.zeppelin.wordcloud;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.zeppelin.helium.Application;
import org.apache.zeppelin.helium.ApplicationArgument;
import org.apache.zeppelin.helium.ApplicationException;
import org.apache.zeppelin.helium.Signal;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.data.TableData;
import org.apache.zeppelin.interpreter.dev.ZeppelinApplicationDevServer;
import org.apache.zeppelin.resource.ResourceKey;



/**
 * THis wordcloud application is based on
 * https://gist.github.com/galleon/5d48e8cec78582b01c0e
 */
public class WordCloud extends Application {

  @Override
  protected void onChange(String name, Object oldObject, Object newObject) {
  }

  @Override
  public void signal(Signal signal) {

  }

  @Override
  public void load() throws ApplicationException, IOException {

  }

  @Override
  public void run(ApplicationArgument arg, InterpreterContext context) throws ApplicationException,
      IOException {
    // get TableData
    TableData tableData = (TableData) context.getResourcePool().get(
        arg.getResource().location(), arg.getResource().name());

    if (tableData == null) {
      context.out.write("No table data found");
      return;
    }

    if (tableData.getColumnDef().length < 2) {
      context.out.write("Minimum 2 columns are required. Word at the first column, frequency at the second column");
      return;
    }
    // wordcloud elementId
    String elementId = "wordcloud_" + context.getParagraphId();

    // create element
    context.out.write("<div id=\"" + elementId + "\" style=\"height:400px;\"></div>");

    // include library
    context.out.write("<script>");
    context.out.writeResource("wordcloud/d3.layout.cloud.js");

    // write data
    int numRows = tableData.length();
    String jsonData = "[";
    for (int i = 0; i < numRows; i++) {
      jsonData += "{ text:\"" + tableData.getData(i, 0) + "\", size:" + Integer.parseInt((String) tableData.getData(i, 1)) + "}";
      if (i != numRows) {
        jsonData += ",";
      }
    }
    jsonData += "]";


    context.out.write("(function() {");
    context.out.write("var elementId = \"" + elementId + "\";");
    context.out.write("var data = " + jsonData + ";");
    context.out.writeResource("wordcloud/draw.js");
    context.out.write("})();");

    context.out.write("</script>");
  }

  @Override
  public void unload() throws ApplicationException, IOException {

  }

  private static String generateData(int num) throws IOException {
    InputStream ins = ClassLoader.getSystemResourceAsStream("wordcloud/mockdata.txt");
    String data = IOUtils.toString(ins);
    String[] dic = data.split("\n");
    Random rand = new Random();

    String tableOutput = "%table world\tfrequency";

    for (int i = 0; i < num; i++) {
      String word = dic[rand.nextInt(dic.length)];
      // word \t size
      tableOutput += "\n" + word + "\t" + (rand.nextInt(90) + 10);
    }

    return tableOutput;
  }


  /**
   * Development mode
   * @param args
   * @throws Exception
   */
  public static void main(String [] args) throws Exception {
    // create development server
    ZeppelinApplicationDevServer dev = new ZeppelinApplicationDevServer(WordCloud.class.getName());

    TableData tableData = new TableData(new InterpreterResult(Code.SUCCESS,
        generateData(40)));

    dev.server.getResourcePool().put("tabledata", tableData);

    // set application argument
    ApplicationArgument arg = new ApplicationArgument(new ResourceKey(
        dev.server.getResourcePoolId(),
        "tabledata"
        ));
    dev.setArgument(arg);

    // start
    dev.server.start();
    dev.server.join();
  }
}
