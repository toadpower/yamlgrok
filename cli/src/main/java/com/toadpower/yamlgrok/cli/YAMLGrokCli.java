package com.toadpower.yamlgrok.cli;

import com.toadpower.yamlgrok.lib.YAMLGrok;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class YAMLGrokCli {

    private boolean verbose = false;

    public static void main(String[] args) {
        YAMLGrokCli yamlGrokCli = new YAMLGrokCli();
        yamlGrokCli.run(args);
    }

    public void run(String[] args) {
        File sourceDir = getSourceDir(args);

        Collection<File> allFiles = getAllFiles(sourceDir);

        YAMLGrok yg = YAMLGrok.create(sourceDir, allFiles);
        try {
            if(yg.grok()) {
                if (verbose) {
                    System.out.println("Found Things:");
                    for (YAMLGrok.Thing thing : yg.foundThingSet) {
                        System.out.println("  " + thing);
                    }
                    System.out.println("Found Links:");
                    for (YAMLGrok.Link link : yg.foundLinkSet) {
                        System.out.println("  " + link);
                    }
                }
                System.exit(0);
            } else {
                if (verbose) {
                    System.out.println("Found Things:");
                    for (YAMLGrok.Thing thing : yg.foundThingSet) {
                        System.out.println("  " + thing);
                    }
                    System.out.println("Need Things:");
                    for (YAMLGrok.Thing thing : yg.needThingSet) {
                        System.out.println("  " + thing);
                    }
                    System.out.println("Found Links:");
                    for (YAMLGrok.Link link : yg.foundLinkSet) {
                        System.out.println("  " + link);
                    }
                    System.out.println("Need Links:");
                    for (YAMLGrok.Link link : yg.needLinkSet) {
                        System.out.println("  " + link);
                    }
                }
                if (!yg.missingThingSet.isEmpty()) {
                    System.out.println("Missing Things:");
                    for (YAMLGrok.Thing thing : yg.missingThingSet) {
                        System.out.println("  " + thing);
                    }
                }
                if (!yg.missingLinkSet.isEmpty()) {
                    System.out.println("Missing Links:");
                    for (YAMLGrok.Link link : yg.missingLinkSet) {
                        System.out.println("  " + link);
                    }
                }
                System.exit(-1);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private File getSourceDir(String[] args) {
        CommandLineParser clp = new DefaultParser();
        try {
            Options options = new Options();
            options.addOption(Option.builder("s")
                .hasArg(true)
                .build());
            options.addOption(Option.builder("v")
                .build());
            CommandLine cl = clp.parse(options, args);
            String sourceDirName = cl.getOptionValue("s");
            File sourceDir = new File(sourceDirName);
            if (!sourceDir.exists()) {
                System.out.println(sourceDirName+" does not exist");
                System.exit(-1);
            }
            if (!sourceDir.isDirectory()) {
                System.out.println(sourceDirName+" is not a directory");
                System.exit(-1);
            }
            verbose = cl.hasOption("v");
            return sourceDir;
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    FileFilter yamlFiles = new FileFilter() {
        public boolean accept(File pathname) {
            if (pathname.isDirectory())
                return false;
            if (pathname.getName().endsWith(".yaml"))
                return true;
            if (pathname.getName().endsWith(".yml"))
                return true;
            return false;
        }
    };

    FileFilter directories = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    private Collection<File> getAllFiles(File sourceDir) {
        Collection<File> allFiles = new ArrayList<File>();
        allFiles.addAll(Arrays.asList(sourceDir.listFiles(yamlFiles)));
        for(File directory: sourceDir.listFiles(directories)) {
            allFiles.addAll(getAllFiles(directory));
        }
        return allFiles;
    }

}
