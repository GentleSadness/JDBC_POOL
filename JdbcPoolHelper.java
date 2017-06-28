import com.utils.Const;
import com.utils.Prop;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/5/18.
 */
public class JdbcPoolHelper {

    private static Logger logger = Logger.getLogger(JdbcPoolHelper.class);

    static Map<String , List<Connection>> connectionMap = new HashMap<String, List<Connection>>();

    public static Connection getConnection(String schemaName) {
        schemaName = schemaName.toLowerCase();
        try {
            List<Connection> connectionList = connectionMap.get(schemaName);
            if (connectionList == null){
                initConnectionPool(schemaName);
                connectionList = connectionMap.get(schemaName);
            }
            synchronized (connectionList){
                if (connectionList.size() == 0){
                    logger.info(schemaName.toUpperCase() + "暂无可用数据库连接！");
                    connectionList.wait(5000);
                    if (connectionList.size() == 0){    //等待5秒后，若还无连接，新建一个，返回
                        Connection connection = getConnection1(schemaName, connectionList);
                        return connection;
                    }
                }
                Connection connection = connectionList.get(0);
                connectionList.remove(0);
                if (connection.isClosed()){
                    connection = getConnection1(schemaName, connectionList);
                }
                return connection;
            }
        }catch (Exception e){
            logger.info("获取数据库连接出错", e);
        }
        return null;
    }


    private static void initConnectionPool(String schemaName){
        List<Connection> connectionList = new LinkedList<Connection>();

        Prop p = new Prop(Const.DEFAULT_PROPERTIES);
        int min_size = p.get("min_size");
        for (int i = 0; i < min_size ; ++i){
            connectionList.add(getConnection1(schemaName, connectionList));
        }
        connectionMap.put(schemaName, connectionList);
    }

    private static Connection getConnection1(String schema, List<Connection> connectionList) {
        try {
            //读取配置文件
            Prop p = new Prop(Const.DEFAULT_PROPERTIES);
            String jdbcUrl = p.get("jdbc." + schema + ".url");
            String user = p.get("jdbc." + schema + ".username");
            String password = p.get("jdbc." + schema + ".password");
            String driverClass = p.get("jdbc." + schema + ".driver");
            String isEncode = p.get("jdbc.pwd_encode");
            String decryptor = p.get("jdbc.pwd_decrypter");

            //解密密码
            if ("true".equals(isEncode)) {
                password = decode(password, decryptor);
            }
            Class.forName(driverClass);
            Connection connection = (Connection) new ConnectionPoolHandler(DriverManager.getConnection(jdbcUrl, user, password), connectionList).newInstance();
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Get Connection fail ", e);
        }
    }

    private static String decode(String encodePWD, String decryptor) {
        String decodePWD = "";
        try {
            Encryptor encryptor = (Encryptor) Class.forName(decryptor).newInstance();
            decodePWD = encryptor.decode(encodePWD);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decodePWD;
    }
}
