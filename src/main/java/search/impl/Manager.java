package search.impl;

import main.SearchManager;
import search.FileHandler;
import search.Parser;
import search.WebSpider;
import vo.Program;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 请在此类中完成自己对象初始化工作，并注册
 */
public class Manager {
    public static FileHandler fileHandler;
    public static Parser parser;
    public static WebSpider webSpider;
    static String sendGet(String url)
    { // 定义一个字符串用来存储网页内容
        String result = "";
        // 定义一个缓冲字符输入流
        BufferedReader in = null;
        try
        {
            // 将string转成url对象
            URL realUrl = new URL(url);
            // 初始化一个链接到那个url的连接
            URLConnection connection = realUrl.openConnection();
            // 开始实际的连接
            connection.connect();
            // 初始化 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            // 用来临时存储抓取到的每一行的数据
            String line;
            while ((line = in.readLine()) != null)
            {
                // 遍历抓取到的每一行并将其存储到result里面
                result += line;
            }
        } catch (Exception e)
        {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        } // 使用finally来关闭输入流
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
            } catch (Exception e2)
            {
                e2.printStackTrace();
            }
        }
        return result;
    }

    static{
        // TODO:在此初始化所需组件，并将其注册到SearchManager中供主函数使用
        // SearchManager.registFileHandler(new yourFileHandler());
        // SearchManager.registSpider(new yourSpider());


//        fileHandler = new FileHandler() {
//            @Override
//            public int program2File(List<Program> programList) {
////
//            }
//        };
        SearchManager.registFileHandler(new FileHandler() {
            @Override
            public int program2File(List<Program> programList) {
                byte[] buff = new byte[]{};
                for(Program program : programList){

                    try {
                        OutputStream os = new FileOutputStream("D:\\programs.txt");
                        String str = program.getSchool()+"\n";
                        str += program.getProgramName()+"\n";
                        str += program.getDegree()+"\n";
                        str += program.getLocation()+"\n";
                        str += program.getHomepage()+"\n";
                        str += program.getEmail()+"\n";
                        str += program.getPhoneNumber();
                        buff = str.getBytes();
                        os.write(buff,0,buff.length);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return 0;
            }
        });


        SearchManager.registSpider(new WebSpider() {
            @Override
            public Parser getParser() {
                Parser parser = new Parser() {
                    @Override
                    public Program parseHtml(String html) {
                        String programPage = sendGet(html);
                        List<String> moreLink = new LinkedList<String>();
                        Pattern pP = Pattern.compile("\\s*|\t|\r|\n");
                        Matcher pm = pP.matcher(programPage);
                        programPage = pm.replaceAll("");
                        Program programInfo = new Program();
                        Pattern programName = Pattern.compile("<divclass=\"shrinkwrap\"><h3>(.*?)</h3></div>");
                        Matcher nameMacth = programName.matcher(programPage);
                        if (nameMacth.find()) {
                            programInfo.setProgramName(nameMacth.toMatchResult().group(1));
                            System.out.println(nameMacth.toMatchResult().group(1));
                        }
                        Pattern school = Pattern.compile("<h1>Overview</h1><p>.*?<ul><li>(.*?)</li><li>");
                        Matcher schoolMatcher = school.matcher(programPage);
                        if (schoolMatcher.find()) {
                            programInfo.setSchool(schoolMatcher.toMatchResult().group(1));
                            System.out.println(schoolMatcher.toMatchResult().group(1));
                        }

                        Pattern details = Pattern.compile("<h2>.*?Degrees.*?Offered.*?</h2><p>.*?<ul>(.*?)</ul>");
                        Pattern detailsN = Pattern.compile("<h2>Certificates.*?Offered</h2><p>.*?<ul>(.*?)</ul>");
                        Matcher detMatcher = details.matcher(programPage);
                        Matcher detMatcherN = detailsN.matcher(programPage);

                        if (detMatcher.find()) {
                            String Link = detMatcher.toMatchResult().group(1);
//              Pattern moreDetails = Pattern.compile("<li><ahref=\"(.*?)\">(.*?)</a>.*?</li>");
                            Pattern moreDetails = Pattern.compile("<li><a.*?href=\"(.*?)\">(.*?)</a>.*?</li>");
                            Matcher moreMatcher = moreDetails.matcher(Link);
                            while (moreMatcher.find()) {
                                int t = moreMatcher.toMatchResult().groupCount();
                                moreLink.add(moreMatcher.toMatchResult().group(1));
                                programInfo.setDegree(moreMatcher.toMatchResult().group(2));
                                System.out.println(moreMatcher.toMatchResult().group(0));

                            }
                        }
                        else if(detMatcherN.find()){
                            String Link = detMatcherN.toMatchResult().group(1);
                            Pattern moreDetails = Pattern.compile("<li><a.*?href=\"(.*?)\">(.*?)</a>.*?</li>");
                            Matcher moreMatcher = moreDetails.matcher(Link);
                            while (moreMatcher.find()) {
                                int t = moreMatcher.toMatchResult().groupCount();
                                moreLink.add(moreMatcher.toMatchResult().group(1));
                                programInfo.setDegree(moreMatcher.toMatchResult().group(2));
                                System.out.println(moreMatcher.toMatchResult().group(0));

                            }
                        }
                        String detailsPage="";
                        if (moreLink.size() > 1) {
                            detailsPage = sendGet(moreLink.get(1));

                        } else if(moreLink.size()==1){
                            detailsPage = sendGet(moreLink.get(0));
                        }
                        else{
                            detailsPage="";
                        }
                        Pattern dP = Pattern.compile("\\s*|\t|\r|\n");
                        Matcher dm = dP.matcher(detailsPage);
                        detailsPage = dm.replaceAll("");
                        Pattern homePagePt = Pattern.compile("<liclass=\"globe-icon\"><ahref=\"(.*?)\"");
                        Pattern deadLine = Pattern.compile("<h3>Department.*?Deadlines</h3>(.*?)</ul>");
                        Pattern location = Pattern.compile("<spanclass=\"ul-wrap\"><strong>(.*?)</strong></span>");
                        Pattern EMail = Pattern.compile("<liclass=\"phone-icon\"><ahref=\".*?\">(.*?)</a></li>");
                        Pattern phone = Pattern.compile("<liclass=\"email-icon\"><ahref=\".*?\">(.*?)</a></li>");
                        Matcher homePageMatcher = homePagePt.matcher(detailsPage);
                        Matcher deadLineMatcher = deadLine.matcher(detailsPage);
                        Matcher EMailMatcher = EMail.matcher(detailsPage);
                        Matcher locationMatcher = location.matcher(detailsPage);
                        Matcher phoneMatcher = phone.matcher(detailsPage);

                        if(homePageMatcher.find()){
                            programInfo.setHomepage(homePageMatcher.toMatchResult().group(1));
                            System.out.println(homePageMatcher.toMatchResult().group(1));
                        }
                        else programInfo.setHomepage(null);
                        if(deadLineMatcher.find()){
                            programInfo.setDeadlineWithAid(deadLineMatcher.toMatchResult().group(1));
                            programInfo.setDeadlineWithoutAid(deadLineMatcher.toMatchResult().group(1));

                        }
                        else {
                            programInfo.setDeadlineWithAid(null);
                            programInfo.setDeadlineWithoutAid(null);
                        }

                        if(locationMatcher.find()){
                            programInfo.setLocation(locationMatcher.toMatchResult().group(1));
                        }
                        else  programInfo.setLocation(null);

                        while(EMailMatcher.find()){
                            programInfo.setEmail(EMailMatcher.toMatchResult().group(1));
                        }
                        while(phoneMatcher.find()){
                            programInfo.setPhoneNumber(phoneMatcher.toMatchResult().group(1));
                        }

                        return programInfo;
                    }
                };
                return parser;
            }

            @Override
            public List<String> getHtmlFromWeb() {
                List<String> programList = new LinkedList<String>();
                List<Program> programs = new LinkedList<Program>();
                // 定义即将访问的链接
                String url = "http://graduateschool.colostate.edu/programs/";
                // 访问链接并获取页面内容
                String mainPage = sendGet(url);
                Pattern p = Pattern.compile("\\s*|\t|\r|\n");
                Matcher m = p.matcher(mainPage);
                mainPage = m.replaceAll("");
//        Pattern pattern = Pattern.compile("<div class=\"programs-div\"><a href=\"(.*)\">");
                Pattern pattern = Pattern.compile("<divclass=\"programs-div\"><ahref=\"(.*?)\"");
                Matcher matcher = pattern.matcher(mainPage);
                MatchResult ms = null;
                while (matcher.find()) {
                    ms = matcher.toMatchResult();
                    int t = ms.groupCount();
                    for (int i = 0; i < t; i++) {
//                System.out.print(String.format("\n 233",i+1,ms.group(i+1)));
                        System.out.print(ms.group(i+1)+"\n");
                        programList.add(ms.group(i+1));
                    }

//            System.out.println(ms.group());
                }

                return programList;
            }
        });

//        parser = new Parser() {
//            @Override
//            public Program parseHtml(String html) {
//
//                return null;
//            }
//        };

    }
}
