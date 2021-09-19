package edu.brown.cs.student.main;

import java.io.*;
import java.util.*;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import spark.ExceptionHandler;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.TemplateViewRoute;
import spark.template.freemarker.FreeMarkerEngine;

/**
 * The Main class of our project. This is where execution begins.
 */
public final class Main {

  // use port 4567 by default when running server
  private static final int DEFAULT_PORT = 4567;

  class Star {
    int StarID;
    String ProperName;
    double X;
    double Y;
    double Z;

    public Star(int starID, String properName, double x, double y, double z) {
      StarID = starID;
      ProperName = properName;
      X = x;
      Y = y;
      Z = z;
    }
  }

  /**
   * The initial method called when execution begins.
   *
   * @param args An array of command line arguments
   */
  public static void main(String[] args) {
    new Main(args).run();
  }

  private String[] args;

  private Main(String[] args) {
    this.args = args;
  }

  private void run() {
    // set up parsing of command line flags
    OptionParser parser = new OptionParser();

    // "./run --gui" will start a web server
    parser.accepts("gui");

    // use "--port <n>" to specify what port on which the server runs
    parser.accepts("port").withRequiredArg().ofType(Integer.class)
            .defaultsTo(DEFAULT_PORT);

    OptionSet options = parser.parse(args);
    if (options.has("gui")) {
      runSparkServer((int) options.valueOf("port"));
    }
    HashMap<String, Star> hMap = new HashMap<String, Star>();
    // TODO: Add your REPL here!
    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
      String input;
      while ((input = br.readLine()) != null) {
        try {
          input = input.trim();
          String[] arguments = input.split(" ");
//          System.out.println(arguments[0]);

          if (arguments[0].equals("add")) {
            MathBot mathbot = new MathBot();
            double result = mathbot.add(Double.valueOf(arguments[1]), Double.valueOf(arguments[2]));
            System.out.println(result);
          } else if (arguments[0].equals("subtract")) {
            MathBot mathbot = new MathBot();
            double result = mathbot.subtract(Double.valueOf(arguments[1]), Double.valueOf(arguments[2]));
            System.out.println(result);
          } else if (arguments[0].equals("stars")) {
            FileInputStream fis = new FileInputStream(arguments[1]);
            BufferedReader br2 = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            int count = 0;
            while ((line = br2.readLine()) != null) {
              if (count == 0) {
                count++;
                continue;
              }
              String[] datas = line.split(",");
              hMap.put(datas[1], new Star(Integer.parseInt(datas[0]), datas[1], Double.parseDouble(datas[2]), Double.parseDouble(datas[3]), Double.parseDouble(datas[4])));
              count++;
            }
            System.out.println("Read " + (count - 1) + " stars from " + arguments[1]);
            br.close();
          } else if (arguments[0].equals("naive_neighbors")) {
            int n = Integer.parseInt(arguments[1]);
            if (n == 0) continue;
            List<Map.Entry<Integer, Double>> ids = new ArrayList<Map.Entry<Integer, Double>>(distances.entrySet());
            if (arguments.length == 3) {
              Star star = hMap.get(arguments[2]);
              HashMap<Integer, Double> distances = new HashMap<Integer, Double>();
              for (Star i : hMap.values()) {
                double d = (i.X - star.X) * (i.X - star.X) + (i.Y - star.Y) * (i.Y - star.Y) + (i.Z - star.Z) * (i.Z - star.Z);
                distances.put(i.StarID, d);
              }
            } else {
              HashMap<Integer, Double> distances = new HashMap<Integer, Double>();
              double X = Double.parseDouble(arguments[2]);
              double Y = Double.parseDouble(arguments[3]);
              double Z = Double.parseDouble(arguments[4]);
              for (Star i : hMap.values()) {
                double d = (i.X - X) * (i.X - X) + (i.Y - Y) * (i.Y - Y) + (i.Z - Z) * (i.Z - Z);
                distances.put(i.StarID, d);
              }
            }
            ids.sort(new Comparator<Map.Entry<Integer, Double>>() {
              @Override
              public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
              }
            });
            for (int i = 0; i < n; i++)
              System.out.println(ids.get(i).getKey());

          }

          // TODO: complete your REPL by adding commands for addition "add" and subtraction
          //  "subtract"
        } catch (Exception e) {
          // e.printStackTrace();
          System.out.println("ERROR: We couldn't process your input");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("ERROR: Invalid input for REPL");
    }

  }

  private static FreeMarkerEngine createEngine() {
    Configuration config = new Configuration(Configuration.VERSION_2_3_0);

    // this is the directory where FreeMarker templates are placed
    File templates = new File("src/main/resources/spark/template/freemarker");
    try {
      config.setDirectoryForTemplateLoading(templates);
    } catch (IOException ioe) {
      System.out.printf("ERROR: Unable use %s for template loading.%n",
              templates);
      System.exit(1);
    }
    return new FreeMarkerEngine(config);
  }

  private void runSparkServer(int port) {
    // set port to run the server on
    Spark.port(port);

    // specify location of static resources (HTML, CSS, JS, images, etc.)
    Spark.externalStaticFileLocation("src/main/resources/static");

    // when there's a server error, use ExceptionPrinter to display error on GUI
    Spark.exception(Exception.class, new ExceptionPrinter());

    // initialize FreeMarker template engine (converts .ftl templates to HTML)
    FreeMarkerEngine freeMarker = createEngine();

    // setup Spark Routes
    Spark.get("/", new MainHandler(), freeMarker);
  }

  /**
   * Display an error page when an exception occurs in the server.
   */
  private static class ExceptionPrinter implements ExceptionHandler<Exception> {
    @Override
    public void handle(Exception e, Request req, Response res) {
      // status 500 generally means there was an internal server error
      res.status(500);

      // write stack trace to GUI
      StringWriter stacktrace = new StringWriter();
      try (PrintWriter pw = new PrintWriter(stacktrace)) {
        pw.println("<pre>");
        e.printStackTrace(pw);
        pw.println("</pre>");
      }
      res.body(stacktrace.toString());
    }
  }

  /**
   * A handler to serve the site's main page.
   *
   * @return ModelAndView to render.
   * (main.ftl).
   */
  private static class MainHandler implements TemplateViewRoute {
    @Override
    public ModelAndView handle(Request req, Response res) {
      // this is a map of variables that are used in the FreeMarker template
      Map<String, Object> variables = ImmutableMap.of("title",
              "Go go GUI");

      return new ModelAndView(variables, "main.ftl");
    }
  }
}
