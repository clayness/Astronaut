package edu.virginia.cs.Framework.Types;

import edu.virginia.cs.AppConfig;
import edu.virginia.cs.Evaluator.ScriptRunner;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

class MySQLMeasurementFunction extends MeasurementFunctionByDB implements Serializable {
    private final Boolean isDebugOn = AppConfig.getDebug();

    final String mysqlUser = AppConfig.getMySQLUser();
    final String mysqlPassword = AppConfig.getMysqlPassword();
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
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public void createDB() {
        System.out.println("createDB function in DBConcreteTimeMeasurementFunction");

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
            //noinspection CallToPrintStackTrace
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
            //noinspection ResultOfMethodCallIgnored
            new File(scriptFileName).delete();
        } catch (IOException | InterruptedException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public void createTables() {
        System.out.println("createTables function in DBConcreteTimeMeasurementFunction");
        String implPath = this.impl.getImPath();
        String scriptFileName = implPath.substring(0,
                implPath.lastIndexOf(File.separator))
                + "createTables.sql";

        try {
            PrintWriter pw = new PrintWriter(scriptFileName);
            String outToFile = this.mysqlCMD + " < " + implPath;
            pw.println(outToFile);
            pw.close();
        } catch (FileNotFoundException e) {
            //noinspection CallToPrintStackTrace
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
            //noinspection ResultOfMethodCallIgnored
            new File(scriptFileName).delete();
        } catch (IOException | InterruptedException e) {
            //noinspection CallToPrintStackTrace
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

            if (insertPath.isEmpty()) {
                continue;
            }
            String imPath = this.impl.getImPath();
            String dbName = imPath.substring(
                    imPath.lastIndexOf(File.separator) + 1,
                    imPath.lastIndexOf("."));
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
                //noinspection CallToPrintStackTrace
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
            if (selectPath.isEmpty()) {
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
                //noinspection CallToPrintStackTrace
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
