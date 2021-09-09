package edu.virginia.cs.Framework.Types;

import edu.virginia.cs.AppConfig;
import edu.virginia.cs.Evaluator.ScriptRunner;
import edu.virginia.cs.Uniq.Pair;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class DBConcreteMeasurementFunction implements Serializable {
    //	private ArrayList<ConcreteLoad> loads;
    //	private DBImplementation impl;
    public MeasurementFunctionByDB mfByDB = null;
    private HashMap<String, HashMap<String, ArrayList<Pair<String>>>> instances;

    public DBConcreteMeasurementFunction() {
        if (AppConfig.getTestDB().equalsIgnoreCase("mysql")) {
            this.mfByDB = new MySQLMeasurementFunction();
        } else if (AppConfig.getTestDB().equalsIgnoreCase("postgres")) {
            this.mfByDB = new PostgresMeasurementFunction();
        } else {
            System.out.println("DBConcreteMeasurementFunction: error...");
            System.out.println("DBConcreteMeasurementFunction: Non-supported RDBMS");
            System.exit(-1);
        }
    }

    public HashMap<String, HashMap<String, ArrayList<Pair<String>>>> getInstances() {
        return instances;
    }

    public void setInstances(HashMap<String, HashMap<String, ArrayList<Pair<String>>>> ins) {
        this.instances = ins;
    }

    public void setImpl(DBImplementation impl) {
        this.mfByDB.setImpl(impl);
    }

    public ArrayList<ConcreteLoad> getLoads() {
        return this.mfByDB.getLoads();
    }

    public void setLoads(ArrayList<ConcreteLoad> loads) {
        this.mfByDB.setLoads(loads);
    }

}

abstract class MeasurementFunctionByDB implements Serializable {

    protected ArrayList<ConcreteLoad> loads;
    protected DBImplementation impl;

    public ArrayList<ConcreteLoad> getLoads() {
        return loads;
    }

    public void setLoads(ArrayList<ConcreteLoad> loads) {
        this.loads = loads;
    }

    public void setImpl(DBImplementation impl) {
        this.impl = impl;
    }

    public abstract double checkSpace();

    public abstract void createDB();

    public abstract void dropDB();

    public abstract void createTables();

    public abstract double runInsert();

    public abstract double runSelect();

}

class MySQLMeasurementFunction extends MeasurementFunctionByDB implements Serializable {
    final String mysqlUser = AppConfig.getMySQLUser();
//	private HashMap<String, HashMap<String, ArrayList<CodeNamePair>>> instances;
    final String mysqlPassword = AppConfig.getMysqlPassword();
    //	private MeasurementType mType = null;
    private final Boolean isDebugOn = AppConfig.getDebug();
    private final String mysqlCMD = "mysql --user='" + mysqlUser + "' --password='" + mysqlPassword + "'";

    public double checkSpace() {
        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));

        String mysqlStat = "select table_schema, sum((data_length+index_length)/1024) AS KB from information_schema.tables where table_schema='" + dbName + "' group by 1;";
        String[] command = new String[]{
                "bash",
                "-c",
                this.mysqlCMD + " -Bse\"" + mysqlStat + "\""};
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] splited = line.split("\\s+");
                if (splited.length == 2) {
                    if (splited[0].equalsIgnoreCase(dbName)) { // find right
                        // data base
                        return Double.parseDouble(splited[1]);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Get space consumption ERROR: " + e.getMessage());
        }
        return -1.0;
    }

    public void dropDB() {
        /*
          get the name from implPath create drop database script :
          "drop database implName;" create cmd to drop the database execute the
          cmd
         */
        System.out.println("dropDB function in DBConcreteTimeMeasurementFunction");
        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));
        String[] command = new String[]{
                "bash", "-c", this.mysqlCMD + " -Bse\"drop database " + dbName + ";\""};

        try {
            System.out.println("Prepare to execute command");
            Process p = Runtime.getRuntime().exec(command);
            System.out.println("Wait for command to finish");
            p.waitFor();
            System.out.println("Command finished");
            if (p.exitValue() != 0) {
                if (isDebugOn) {
                    System.out.println("Drop DB Failure...");
                    System.out.println("DropDB: exit value = " + p.exitValue());
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createDB() {
        System.out.println("createDB function in DBConcreteTimeMeasurementFunction");
        /*
          get the name from implPath create create database script :
          "create database implName;" create cmd to create the database execute
          the cmd
         */

        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));
        String createDatabase = "create database " + dbName + ";";
        String scriptFileName = implPath.substring(0,
                implPath.lastIndexOf(File.separator))
                + "createDatabase.sql";
        try {
            PrintWriter pw = new PrintWriter(scriptFileName);
            String outToFile = this.mysqlCMD + " -Bse " + "\"" + createDatabase
                    + "\"";
            pw.println(outToFile);
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Process p = Runtime.getRuntime().exec("bash " + scriptFileName);
            p.waitFor();
            if (p.exitValue() != 0) {
                if (isDebugOn) {
                    System.out.println("Created DB Failure...");
                    System.out.println("Create DB: exit value = "
                            + p.exitValue());
                }
            }
            // delete the script
            new File(scriptFileName).delete();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createTables() {
        System.out.println("createTables function in DBConcreteTimeMeasurementFunction");
        /*
          get the name from implPath create create tables script call bash to
          execute that script
         */
        String implPath = this.impl.getImPath();
        // String dbName =
        // implPath.substring(implPath.lastIndexOf(File.separator)+1,
        // implPath.lastIndexOf("."));
        String scriptFileName = implPath.substring(0,
                implPath.lastIndexOf(File.separator))
                + "createTables.sql";

        try {
            PrintWriter pw = new PrintWriter(scriptFileName);
            String outToFile = this.mysqlCMD + " < " + implPath;
            pw.println(outToFile);
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Process p = Runtime.getRuntime().exec("bash " + scriptFileName);
            p.waitFor();
            if (p.exitValue() != 0) {
                if (isDebugOn) {
                    System.out.println("Create schema Failure...");
                    System.out.println("Create Schema: exit value = "
                            + p.exitValue());
                }
            }
            // delete the script
            new File(scriptFileName).delete();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public double runInsert() {
        System.out.println("runInsert function in DBConcreteTimeMeasurementFunction");
        // iterate all concrete load
        // record start time
        // run scripts
        // record end time
        // return end-start
        long insertInterval = -1;
        for (ConcreteLoad cl : this.loads) {
            String insertPath = cl.getInsertPath();

            if (insertPath.length() == 0) {
                continue;
            }
            String imPath = this.impl.getImPath();
            String dbName = imPath.substring(
                    imPath.lastIndexOf(File.separator) + 1,
                    imPath.lastIndexOf("."));
//			String[] cmd = new String[] { "mysql", dbName, "-u" + mysqlUser,
//					"-p" + mysqlPassword, "-e", "source " + insertPath };
            long startTime = System.currentTimeMillis();

            // Connection conn=getConnection();//some method to get a Connection
            if (isDebugOn) {
                System.out.println("Insert start----" + dbName);
            }
            Connection conn;
            try {
//				Class.forName("com.mysql.jdbc.Driver");
                String connStr = "jdbc:mysql://localhost/" + dbName + "?useSSL=false";
                conn = DriverManager.getConnection(connStr, mysqlUser, mysqlPassword);
                ScriptRunner runner = new ScriptRunner(conn, false, false);
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(insertPath));
                runner.runScript(reader);
                reader.close();
                conn.close();
            } catch (SQLException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            long endTime = System.currentTimeMillis();
            insertInterval = endTime - startTime;
            if (isDebugOn) {
                System.out.println("Spend time: "
                        + TimeUnit.MILLISECONDS.toSeconds(insertInterval));
                System.out.println("Insert finished----" + dbName);
            }
        }
        return insertInterval;
    }

    public double runSelect() {
        System.out.println("runSelect function in DBConcreteTimeMeasurementFunction");
        long selectInterval = -1;
        for (ConcreteLoad cl : this.loads) {
            String selectPath = cl.getSelectPath();
            if (selectPath.length() == 0) {
                continue;
            }

            String imPath = this.impl.getImPath();
            String dbName = imPath.substring(
                    imPath.lastIndexOf(File.separator) + 1,
                    imPath.lastIndexOf("."));

            long startTime = System.currentTimeMillis();
            Connection conn;
            try {
//				Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection("jdbc:mysql://localhost/"
                        + dbName + "?user=" + mysqlUser + "&password="
                        + mysqlPassword);//+"&autoReconnect=true&useSSL=false");
                ScriptRunner runner = new ScriptRunner(conn, false, false);
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(selectPath));
                runner.runScript(reader);
                reader.close();
                conn.close();
            } catch (SQLException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
//			try {
//				if (isDebugOn) {
//					System.out.println("Select start----" + dbName);
//				}
//				proc = Runtime.getRuntime().exec(cmd);
//				BufferedReader stdInput = new BufferedReader(
//						new InputStreamReader(proc.getInputStream()));
//
//				BufferedReader stdError = new BufferedReader(
//						new InputStreamReader(proc.getErrorStream()));
//				String s = null;
//				while ((s = stdInput.readLine()) != null) {
//					// System.out.println(s);
//				}
//
//				// read any errors from the attempted command
//				// System.out.println("Here is the standard error of the command (if any):\n");
//				while ((s = stdError.readLine()) != null) {
//					// System.out.println(s);
//				}
//				proc.waitFor();
//				if (proc.exitValue() != 0) {
//					if (isDebugOn) {
//						System.err.println("Select: exit value = "
//								+ proc.exitValue() + "----" + dbName);
//					}
//				}
//
//			} catch (IOException ex) {
//				Logger.getLogger(DBConcreteMeasurementFunction.class.getName())
//						.log(Level.SEVERE, null, ex);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

            long endTime = System.currentTimeMillis();
            selectInterval = endTime - startTime;
            if (isDebugOn) {
                System.out.println("Spend time: "
                        + TimeUnit.MILLISECONDS.toSeconds(selectInterval));
                System.out.println("Select finished----" + dbName);
            }
        }
        return selectInterval;
    }
}

class PostgresMeasurementFunction extends MeasurementFunctionByDB implements Serializable {
    private final Boolean isDebugOn = AppConfig.getDebug();
//    private HashMap<String, HashMap<String, ArrayList<CodeNamePair>>> instances;


    public double checkSpace() {
        System.out.println("checkSpace function in DBConcreteTimeMeasurementFunction");
        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));

//        String[] command = new String[]{
//                "bash",
//                "-c",
//                this.postgresCMD + " " + dbName, "-c", "\"SELECT pg_database_size('"+ dbName +"');\""};
        String[] command = new String[]{"bash", "-c",
                "psql -c \"SELECT pg_database_size('" + dbName.toLowerCase() + "');\" " + dbName.toLowerCase()};
        Process p;
        try {
            System.out.println("Prepare to execute command: Check Space");
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            System.out.println("Wait for command to finish: Check Space");
            /* output will be like
             pg_database_size
             ------------------
                        7217324
             (1 row)

            */
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));

            String line;
            ArrayList<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            for (String s : lines) {
                if (s.startsWith("    ")) {
                    System.out.println("Database: " + dbName + " ; Size: " + s);
                    return Double.parseDouble(s.trim()) / 1024;   // in KB
                }
            }
        } catch (Exception e) {
            System.out.println("Get space consumption ERROR: " + e.getMessage());
        }
        return -1.0;
    }

    public void dropDB() {
        /*
          get the name from implPath create drop database script :
          "drop database implName;" create cmd to drop the database execute the
          cmd
         */
        System.out.println("dropDB function in DBConcreteTimeMeasurementFunction");
        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));

        String[] command = new String[]{"bash", "-c",
                "psql -c \"drop database " + dbName.toLowerCase() + ";\" postgres &> /dev/null"};
//        System.out.println(String.join(" ", command));
        try {
            System.out.println("Prepare to execute command: DROP DB");
            Process p = Runtime.getRuntime().exec(command);

            System.out.println("Wait for command to finish: DROP DB");
            p.waitFor();
            System.out.println("Command finished");
            if (p.exitValue() != 0) {
                if (isDebugOn) {
                    System.out.println("Drop DB Failure...");
                    System.out.println("DropDB: exit value = " + p.exitValue());
//                    System.exit(-1);
                }
            }
//            System.out.println("=========================================");
//            System.out.println("=========================================");
//            System.out.println("===============DROPDB standard output=============");
//            while ((s = stdInput.readLine()) != null) {
//                System.out.println(s);
//            }
//            System.out.println("===============DROPDB ERROR output=============");
//            while ((s = stdError.readLine()) != null) {
//                System.out.println(s);
//            }
//            System.out.println("=========================================");
//            System.out.println("=========================================");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createDB() {
        System.out.println("createDB function in DBConcreteTimeMeasurementFunction");
        /*
          get the name from implPath create create database script :
          "create database implName;" create cmd to create the database execute
          the cmd
         */

        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));

        String[] command = new String[]{"bash", "-c",
                "psql -c \"create database " + dbName.toLowerCase() + ";\" postgres  &> /dev/null"};
        System.out.println(String.join(" ", command));
        try {
            System.out.println("Prepare to execute command: CREATE DB");
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));
            System.out.println("Wait for command to finish: CREATE DB");
            p.waitFor();
            System.out.println("Command finished");
            if (p.exitValue() != 0) {
                if (isDebugOn) {
                    System.out.println("CREATE DB Failure...");
                    System.out.println("CREATEDB: exit value = " + p.exitValue());

                    String s;
                    System.out.println("=========================================");
                    System.out.println("=========================================");
                    System.out.println("===============CREATEDB standard output=============");
                    while ((s = stdInput.readLine()) != null) {
                        System.out.println(s);
                    }
                    System.out.println("===============CREATEDB ERROR output=============");
                    while ((s = stdError.readLine()) != null) {
                        System.out.println(s);
                    }
                    System.out.println("=========================================");
                    System.out.println("=========================================");
                    System.exit(-1);
                }
            }
//            // delete the script
//            new File(scriptFileName).delete();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createTables() {
        System.out.println("createTables function in PostgresMeasurementFunction");
        /*
          get the name from implPath create tables script call bash to
          execute that script
         */
        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));
        String[] command = new String[]{"bash", "-c",
                "psql -f " + implPath + " " + dbName.toLowerCase() + " &> /dev/null"};

        try {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            if (p.exitValue() != 0) {
                if (isDebugOn) {
                    System.out.println("Create schema Failure...");
                    System.out.println("Create Schema: exit value = "
                            + p.exitValue());
                    System.exit(-1);
                }
            }
            // delete the script
//            new File(scriptFileName).delete();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public double runInsert() {
        System.out.println("runInsert function in PostgresMeasurementFunction");
        long insertInterval = -1;
        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));
        for (ConcreteLoad cl : this.loads) {
            String insertPath = cl.getInsertPath();

            if (insertPath.length() == 0) {
                continue;
            }

            long startTime = System.currentTimeMillis();
            String[] command = new String[]{"bash", "-c",
                    "psql -f " + insertPath + " " + dbName.toLowerCase() + " &> /dev/null"};

            try {
                System.out.println("Prepare to execute command: Run Insert");
                Process p = Runtime.getRuntime().exec(command);
                System.out.println("Wait for command to finish: Run Insert");
                p.waitFor();
                if (p.exitValue() != 0) {
                    if (isDebugOn) {
                        System.out.print("Run insert Failure...");
                        System.out.println("exit value = "
                                + p.exitValue());
                        System.exit(-1);
                    }
                }
                // delete the script
//            new File(scriptFileName).delete();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            long endTime = System.currentTimeMillis();
            insertInterval = endTime - startTime;
            if (isDebugOn) {
                System.out.println("Spend time: "
                        + TimeUnit.MILLISECONDS.toSeconds(insertInterval));
                System.out.println("Insert finished----" + dbName);
            }
        }
        return insertInterval;
    }

    public double runSelect() {
        System.out.println("runSelect function in PostgresMeasurementFunction");
        long selectInterval = -1;
        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));
        for (ConcreteLoad cl : this.loads) {
            String selectPath = cl.getSelectPath();
            if (selectPath.length() == 0) {
                continue;
            }

            long startTime = System.currentTimeMillis();
//            String[] cmd = new String[]{"bash", "-c", "\"" + this.postgresCMD + " " + dbName + " -f ", selectPath+"\""};
//            String cmd = this.postgresCMD + " " + dbName + " -f " + selectPath;
            String[] command = new String[]{"bash", "-c",
                    "psql -f " + selectPath + " " + dbName.toLowerCase() + " &> /dev/null"};

            try {
                System.out.println("Prepare to execute command: Run Select");
                Process p = Runtime.getRuntime().exec(command);
                System.out.println("Wait for command to finish: Run Select");
                p.waitFor();
                if (p.exitValue() != 0) {
                    if (isDebugOn) {
                        System.out.print("Run insert Failure...");
                        System.out.println("exit value = "
                                + p.exitValue());
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            long endTime = System.currentTimeMillis();
            selectInterval = endTime - startTime;
            if (isDebugOn) {
                System.out.println("Spend time: "
                        + TimeUnit.MILLISECONDS.toSeconds(selectInterval));
                System.out.println("Select finished----" + dbName);
            }
        }
        return selectInterval;
    }

}

