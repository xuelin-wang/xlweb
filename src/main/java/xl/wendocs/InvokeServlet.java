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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuelin on 2/16/15.
 */
public class InvokeServlet extends HttpServlet {
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
        String userData = req.getParameter("userData");
        JSONObject userDataJson = new JSONObject(userData);
        String result = null;

        String funcName = userDataJson.getString("funcName");

        List<String> strs = new ArrayList<String>();
        int count = 0;
        for (int index = 0; index < 20; index++) {
            String name = "param" + index;
            Object val = userDataJson.opt(name);
            String str;
            if (val == null)
                str = "";
            else {
                str = val.toString();
                count = index + 1;
            }
            strs.add(str);
        }
        String[] strArr = strs.subList(0, count).toArray(new String[count]);

        if ("formatStr".equals(funcName)) {
            result = formatStr(strArr);
        }
        else if ("formatStr2".equals(funcName)) {
            result = formatStr2(strArr);
        }
        else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.append(result);
        out.close();
    }

    private String formatStr(String... params)
    {
        StringBuilder sb = new StringBuilder();
        for (String param: params) {
            sb.append(param);
        }
        return sb.toString();
    }

    private String formatStr2(String... params)
    {
        String str = formatStr(params);
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("str", str);
        return jsonObj.toString();
    }
}
