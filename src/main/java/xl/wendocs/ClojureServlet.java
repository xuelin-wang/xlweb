package xl.wendocs;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by xuelin on 2/16/15.
 */
public class ClojureServlet extends HttpServlet {
    private static boolean _warmedup = false;
    static {
        if (!_warmedup) {
            IFn require = Clojure.var("clojure.core", "require");
            require.invoke(Clojure.read("xl.wendocs"));
            require.invoke(Clojure.read("cheshire.core"));
            _warmedup = true;
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        doGet(req, resp);
    }

    private String[] objsToStrs(Object[] params)
    {
        if (params == null)
            return null;
        String[] paramStrs = new String[params.length];
        for (int index = 0; index < params.length; index++)
            paramStrs[index] = String.valueOf(params[index]);
        return paramStrs;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String reqData = req.getParameter("reqData");
        JSONObject reqJson = new JSONObject(reqData);
        Object result = null;

        String funcName = reqJson.getString("funcName");
        JSONArray paramsArr = reqJson.optJSONArray("params");
        Object[] params;
        if (paramsArr == null) {
            params = new Object[]{};
        }
        else {
            int len = paramsArr.length();
            params = new Object[len];
            for (int index = 0; index < len; index++) {
                params[index] = paramsArr.get(index);
            }
        }

        if ("formatStr".equals(funcName)) {
            String[] paramStrs = objsToStrs(params);
            result = formatStr(paramStrs);
        }
        else if ("formatStr2".equals(funcName)) {
            String[] paramStrs = objsToStrs(params);
            result = formatStr2(paramStrs);
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

    private String formatStr(String... params)
    {
        IFn formatStr = Clojure.var("xl.wendocs", "formatStr");
        return (String)formatStr.invoke(params[0], params[1], params[2]);
    }

    private Object formatStr2(String... params)
    {
        IFn formatStr = Clojure.var("xl.wendocs", "formatStr2");
        return formatStr.invoke(params[0], params[1], params[2]);
    }
}
