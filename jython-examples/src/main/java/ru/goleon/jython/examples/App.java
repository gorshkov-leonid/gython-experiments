package ru.goleon.jython.examples;

import org.python.core.PyInteger;
import org.python.core.PyString;
import org.python.google.common.collect.Lists;
import org.python.util.PythonInterpreter;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

public class App {
    static {
        Properties props = new Properties();
        props.put("jython.console.encoding", "UTF-8"); // todo is need? Used to prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
        props.put("jython.security.respectJavaAccessibility", "false"); //todo is need? don't respect java accessibility, so that we can access protected members on subclasses
        props.put("jython.import.site", "false"); //do not use standalone python libraries
        props.put("python.import.site", "false"); //do not use standalone python libraries
        PythonInterpreter.initialize(System.getProperties(), props, new String[0]);
    }

    private static final PythonInterpreter PYTHON_INTERPRETER = PythonInterpreter.threadLocalStateInterpreter(null);

    private static final int N = 30;

    private static final String INPUT = "# example\n" +
            "name:\n" +
            "  # details\n" +
            "  family: Smith   # very common\n" +
            "  given: Alice    # one of the siblings\n";

    private static final String FUNCTION = "def aaa(text, n):\n" +
            "  yaml = YAML()\n" +
            "  code = yaml.load(text)\n" +
            "  code['name']['given'] = 'Bob '+`n`\n" +
            "  stream = StringIO()\n" +
            "  yaml.dump(code, stream)\n" +
            "  return stream.getvalue()\n" +
            "  #yaml.dump(code, sys.stdout)\n" +
            "  #with open('c:/'+'Bob '+`n`+'.txt', 'w') as f:\n" +
            "  #  yaml.dump(code, f)";

    public static void main(String[] args) throws URISyntaxException, ExecutionException, InterruptedException {
        long g = System.currentTimeMillis();
        PYTHON_INTERPRETER.exec(
                "import sys\n" +
                        "from ruamel.yaml import YAML\n" +
                        "from ruamel.yaml.compat import StringIO \n"

        );
        System.out.println(System.currentTimeMillis() - g);

        //===========================================================

        List<Future<String>> tasks = Lists.newArrayList();
        final List<Long> times = new CopyOnWriteArrayList<Long>();

        ExecutorService executor = Executors.newFixedThreadPool(N * 2);
        final CyclicBarrier barier = new CyclicBarrier(N);
        for (int i = 0; i < N; i++) {
            final int n = i;
            Future<String> task = executor.submit(new Callable<String>() {
                public String call() {
                    try {
                        barier.await();

                        //save time
                        long t = System.currentTimeMillis();

                        PYTHON_INTERPRETER.exec(FUNCTION);
                        PyString res = (PyString) PYTHON_INTERPRETER.get("aaa").__call__(new PyString(INPUT), new PyInteger(n));

                        //time period
                        times.add(System.currentTimeMillis() - t);

                        return res.getString();
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            });
            tasks.add(task);
        }

        int i = -1;
        for (Future<String> task : tasks) {
            String res = task.get();
            System.out.println("=========  " + times.get(++i) + "  =========\n" + res);
        }

        g = System.currentTimeMillis() - g;
        System.out.println("global " + g + "\n" + "========================================");
        System.out.println("global average " + (g / times.size()) + "\n" + "========================================");
        System.out.println("operation average " + avg(times) + "\n" + "========================================");
    }

    private static Long avg(List<Long> times) {
        Long sum = 0L;
        for (Long vals : times) {
            sum += vals;
        }
        return sum / times.size();
    }
}


//        PYTHON_INTERPRETER.exec(
//                "import sys\n" +
//                        "from ruamel.yaml import YAML\n" +
//                        "\n" +
//                        "inp = \"\"\"\\\n" +
//                        "# example\n" +
//                        "name:\n" +
//                        "  # details\n" +
//                        "  family: Smith   # very common\n" +
//                        "  given: Alice    # one of the siblings\n" +
//                        "\"\"\"\n" +
//                        "\n" +
//                        "yaml = YAML()\n" +
//                        "code = yaml.load(inp)\n" +
//                        "code['name']['given'] = 'Bob'\n" +
//                        "\n" +
//                        "yaml.dump(code, sys.stdout)"
//        );

