import org.apache.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2017/5/18.
 */
public class ConnectionPoolHandler implements InvocationHandler {

    private static Logger logger = Logger.getLogger(ConnectionPoolHandler.class);

    //实际要代理的连接
    Connection connection = null;

    List<Connection> connectionList = null;

    List<PreparedStatement> preparedStatementList = new LinkedList<PreparedStatement>();


    public ConnectionPoolHandler(Connection connection, List<Connection> connectionList) {
        this.connection = connection;
        this.connectionList = connectionList;
    }

    public Object newInstance() {
        /**
         * 第一个参数类加载器 第二个参数时要处理的接口 第三个参数当前代理句柄InvocationHandler
         */
        return java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class[]{Connection.class}, this);
    }

    /**
     * 代理
     */
    public Object invoke(Object proxy, Method method, Object[] param)
            throws Throwable {
        Object obj = null;
        //动态代理，拦截close()方法
        if (method.getName().equalsIgnoreCase("close")) {
            for (PreparedStatement preparedStatement : preparedStatementList) {
                preparedStatement.close();
            }
            synchronized (connectionList){

                Prop p = new Prop(Const.DEFAULT_PROPERTIES);
                int max_size = p.get("max_size");

                if (connectionList.size() > max_size){
                    this.connection.close();
                } else {
                    connectionList.add((Connection) proxy);
                    connectionList.notifyAll();
                }

            }
        } else if (method.getReturnType() == PreparedStatement.class) {
            obj = method.invoke(this.connection, param);
            preparedStatementList.add((PreparedStatement) obj);
        } else {
            obj = method.invoke(this.connection, param);    //执行方法
        }
        //返回值
        return obj;
    }
}
