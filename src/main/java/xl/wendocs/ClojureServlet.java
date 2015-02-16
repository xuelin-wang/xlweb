package xl.wendocs;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by xuelin on 2/16/15.
 */
public class ClojureServlet extends HttpServlet {
    private static boolean _inited = false;
    private static void initClojure() {
        if (!_inited) {
            IFn require = Clojure.var("clojure.core", "require");
            require.invoke(Clojure.read("xl.wendocs"));
            require.invoke(Clojure.read("cheshire.core"));
            _inited = true;
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        doGet(req, resp);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        initClojure();
        String action = req.getParameter("action");
        Object result = null;

        if ("formatStr".equals(action)) {
            result = formatStr(req.getParameter("content"));
        }
        else if ("formatStr2".equals(action)) {
            result = formatStr2(req.getParameter("content"));
        }
        else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        String jsonStr =  toJsonStr(result);
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.append(jsonStr);
        out.close();
    }

    private String toJsonStr(Object param)
    {
        IFn jsonWriteStr = Clojure.var("cheshire.core", "generate-string");
        return (String)jsonWriteStr.invoke(param);
    }

    private String formatStr(String content)
    {
        IFn formatStr = Clojure.var("xl.wendocs", "formatStr");
        return (String)formatStr.invoke(content);
    }

    private Object formatStr2(String content)
    {
        IFn formatStr = Clojure.var("xl.wendocs", "formatStr2");
        return formatStr.invoke(content);
    }
}
