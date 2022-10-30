/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, version 2.1, dated February 1999.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the latest version of the GNU Lesser General
 * Public License as published by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program (LICENSE.txt); if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package jgrepsed;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.System.exit;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author rzp16
 */
public class Jgrepsed {

    /**
     * Замена текста в файлах с заданным расширением. Задание набора замен в
     * файле. Поиск и замена текста в файле при помощи регулярных сообщений
     * Jgrepsed -i < input dir> -o < output dir> -e < extention> -t
     * < tasklist file> -r recursively -c < charset> -h help
     *
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        int c;
        //String arg;
        LongOpt[] longopts = new LongOpt[7];
        String extention = null;
        String inputdir = null;
        String outputdir = null;
        String tasklist = null;
        String charset = "Cp1251"; //windows-1251
        boolean recursively = false;
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        longopts[1] = new LongOpt("outputdir", LongOpt.REQUIRED_ARGUMENT, null, 'o');
        longopts[2] = new LongOpt("inputdir", LongOpt.REQUIRED_ARGUMENT, null, 'i');
        longopts[3] = new LongOpt("extention", LongOpt.REQUIRED_ARGUMENT, null, 'e');
        longopts[4] = new LongOpt("tasklist", LongOpt.REQUIRED_ARGUMENT, null, 't');
        longopts[5] = new LongOpt("recursively", LongOpt.REQUIRED_ARGUMENT, null, 'r');
        longopts[6] = new LongOpt("charset", LongOpt.REQUIRED_ARGUMENT, null, 'c');
        // 
        Getopt g = new Getopt("testprog", args, "o:i:e:t:hrc:", longopts);
        //g.setOpterr(true); // We'll do our own error handling
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'c':
                    charset = g.getOptarg();
                    break;
                case 'o':
                    outputdir = g.getOptarg();
                    break;
                case 'i':
                    inputdir = g.getOptarg();
                    break;
                case 'e':
                    extention = g.getOptarg();
                    break;
                case 't':
                    tasklist = g.getOptarg();
                    break;
                case 'r':
                    recursively = true;
                    break;
                case 'h':
                    System.out.println("Jgrepsed -i <input dir> -o <output dir> -e <extention> -t <tasklist file> -r recursively -c < charset> -h help");
                    exit(0);
                    break;
                default:
                    System.out.println("Jgrepsed -i <input dir> -o <output dir> -e <extention> -t <tasklist file> -r recursively -c < charset> -h help");
                    System.out.println("getopt() returned " + c);
                    exit(0);
                    break;
            }
        }
        //
        for (int i = g.getOptind(); i < args.length; i++) {
            System.out.println("Non option args element: " + args[i] + "\n");
        }
        if (inputdir == null || outputdir == null || extention == null || tasklist == null
                || !Files.exists(Paths.get(inputdir)) || !Files.exists(Paths.get(outputdir)) || !Files.exists(Paths.get(tasklist))) {
            System.out.println("\"Jgrepsed -i <input dir> -o <output dir> -e <extention> -t <tasklist file> -r recursively -c <charset> -h help\"");
            System.out.println("Check arguments");
            exit(-1);
        }
        final String inputdirf = inputdir;
        final String extentionfl = extention;
        final String tasklistf = tasklist;
        final String outputdirf = outputdir;
        final String charsetf = charset;
        String[] exts = extentionfl.split(",");
        for (String extentionf : exts) {
            List<String> tasks = Files.readAllLines(Paths.get(tasklistf), Charset.forName(charset));
            if (recursively) {
                Files.walkFileTree(Paths.get(inputdirf), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        //return super.visitFile(file, attrs); 
                        if (file.getFileName().toString().endsWith("." + extentionf)) {
                            processFile(file, inputdirf, outputdirf, extentionf, tasks, charsetf);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        String newsubdirs = dir.toString().substring(inputdirf.length() - 1);

                        if (!new File(outputdirf + newsubdirs).exists()) {
                            new File(outputdirf + newsubdirs).mkdir();
                        }

                        //return super.postVisitDirectory(dir, exc); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                try ( DirectoryStream<Path> dirstream = Files.newDirectoryStream(Paths.get(inputdirf), "*." + extentionf)) {
                    dirstream.forEach((Path path) -> {
                        processFile(path, inputdirf, outputdirf, extentionf, tasks, charsetf);
                    });
                }
            }
        }

    }

    /**
     * Замена текста в файле
     *
     * @param path
     * @param inputdirf
     * @param outputdirf
     * @param extentionf
     * @param tasks
     * @param charset
     */
    public static void processFile(Path path, String inputdirf, String outputdirf, String extentionf, List<String> tasks, String charset) {
        try {
            String s = new String(Files.readAllBytes(path), charset);
            String newsubdirs = path.getParent().toString().substring(inputdirf.length() - 1);
            for (String task : tasks) {
                if (task.startsWith("#")) {
                    continue;
                }
                Pattern p = Pattern.compile(/*new String(args[2].getBytes("CP1251"))*/task.split("=", 2)[0]);
                Matcher m = p.matcher(s);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {//System.out.println(sb.toString()+"!!!");
                    m.appendReplacement(sb, task.split("=", 2)[1].replace("\\n", "\n"));
                }
                m.appendTail(sb);
                s = sb.toString();
            }
            if (!new File(outputdirf + newsubdirs).exists()) {
                new File(outputdirf + newsubdirs).mkdir();
            }
            //Files.write(new File(+path.getFileName()).toPath(), new String(sb.toString().getBytes("UTF-8"), "ISO-8859-1").getBytes(), StandardOpenOption.CREATE);
            try ( FileOutputStream of = new FileOutputStream(/*outputdirf + path.getFileName()*/
                    outputdirf + path.toString().substring(inputdirf.length() - 1), true); //Files.write(new File(+path.getFileName()).toPath(), new String(sb.toString().getBytes("UTF-8"), "ISO-8859-1").getBytes(), StandardOpenOption.CREATE);
                      Writer out = new OutputStreamWriter(of, charset)) {
                out.write(s);
                out.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(Jgrepsed.class.getName()).log(Level.SEVERE, null, ex);
            exit(-1);
        }

    }

}
