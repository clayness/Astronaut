package edu.virginia.cs.Framework.Types;

import edu.virginia.cs.AppConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class PostgresMeasurementFunction extends MeasurementFunctionByDB implements Serializable {
    private final Boolean isDebugOn = AppConfig.getDebug();

    public double checkSpace() {
        System.out.println("checkSpace function in DBConcreteTimeMeasurementFunction");
        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));

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
            List<String> lines = new ArrayList<>();
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
        } catch (IOException | InterruptedException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public void createTables() {
        System.out.println("createTables function in PostgresMeasurementFunction");
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
            //noinspection CallToPrintStackTrace
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

            if (insertPath.isEmpty()) {
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
        System.out.println("runSelect function in PostgresMeasurementFunction");
        long selectInterval = -1;
        String implPath = this.impl.getImPath();
        String dbName = implPath.substring(
                implPath.lastIndexOf(File.separator) + 1,
                implPath.lastIndexOf("."));
        for (ConcreteLoad cl : this.loads) {
            String selectPath = cl.getSelectPath();
            if (selectPath.isEmpty()) {
                continue;
            }

            long startTime = System.currentTimeMillis();
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
