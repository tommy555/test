import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

public class main{

//    DB parameter
    static java.sql.Connection conn = null;
    static PreparedStatement pStmt = null;
    static Statement stmt = null;
    static String url = "jdbc:sqlite:E://fbcrawl.db";

//    SQL Query
    final static String TABLE_NAME = "Post";
    final static String ID_FIELD = "id";
    final static String TITLE_FIELD = "title";
    final static String CONTENT_FIELD = "content";
    final static String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "+TABLE_NAME+" ("+ID_FIELD+" integer PRIMARY KEY AUTOINCREMENT, "+TITLE_FIELD+" text NOT NULL, "+CONTENT_FIELD+" text NOT NULL);";
    final static String INSERT_POST = "INSERT INTO "+TABLE_NAME+" ("+TITLE_FIELD+","+CONTENT_FIELD+") VALUES (?, ?)";
    final static String SELECT_CONTENT = "SELECT * FROM "+TABLE_NAME;

//    Link parameter
    final static String LOGIN_PAGE = "https://m.facebook.com";
    final static String TARGET_PAGE = "https://m.facebook.com/UTARconfessions17";
    final static String ENG_SHOW_MORE = "Show more";
    final static String MY_SHOW_MORE = "Tunjukan Lagi";

    public static void main(String args[]) {
        //        connect to db
        if (!connectDB()) {
            System.out.print("Unable connect to DB");
        }

        System.out.println("1. Load latest post\n2. Load history\n\n");
        Scanner input = new Scanner(System.in);
        int selection = input.nextInt();

        switch (selection) {
            case 1:
                loadPost();
                break;
            case 2:
                showPost();
                break;
            default:
                System.out.println("Invalid selection.");
        }



    }

    public static void loadPost() {
        String LOADED_LINK = "";
        String LINK_SHOW_MORE = ENG_SHOW_MORE;
        int POST_RETRIEVED_AMOUNT = 5;
        String tmp = "";
        ArrayList<String> postsContent = new ArrayList<String>();

        try {
            stmt = conn.createStatement();
            stmt.execute(CREATE_TABLE);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Loading......");
        try {
            String lsd, mts, li, tryNumber, unrecognizedTries;
            String nextURL = "";
            Connection.Response res = Jsoup.connect(LOGIN_PAGE).method(Connection.Method.POST).execute();
            Document loginPage = res.parse();
            String formURL = loginPage.getElementById("login_form").attr("action");
            Map<String, String> cookies = res.cookies();

            lsd = loginPage.select("input[name=lsd]").attr("value");
            mts = loginPage.select("input[name=m_ts]").attr("value");
            li = loginPage.select("input[name=li]").attr("value");
            tryNumber = loginPage.select("input[name=try_number]").attr("value");
            unrecognizedTries = loginPage.select("input[name=unrecognized_tries]").attr("value");

            Connection.Response loginRes = Jsoup.connect(formURL)
                    .userAgent("Mozilla")
                    .data("email", "tommy04081996@gmail.com")
                    .data("pass", "189Aloom")
                    .data("lsd", lsd)
                    .data("m_ts", mts)
                    .data("li", li)
                    .data("try_number", tryNumber)
                    .data("unrecognized_tries", unrecognizedTries)
                    .data("login", "Log+Masuk")
                    .cookies(cookies)
                    .timeout(3000)
                    .method(Connection.Method.POST)
                    .execute();
            cookies = loginRes.cookies();

            Document page;

            nextURL = TARGET_PAGE;


            while(true){
                page = Jsoup
                        .connect(nextURL)
                        .cookies(cookies)
                        .userAgent("Mozilla")
                        .get();

                Elements posts = page.getElementsByTag("p");
                for (Element e : posts) {
                    if (postsContent.size() < POST_RETRIEVED_AMOUNT) {
                        if (e.text().charAt(0) == '#') {
                            if (!tmp.equals("")){
                                postsContent.add(tmp);
                            }
                            tmp = e.text();
                        }
                        else {
                            tmp += e.text();
                        }
                    }
                    else break;
                }

                if (postsContent.size() >= POST_RETRIEVED_AMOUNT) break;

                Elements links = page.getElementsByTag("a");

                for (Element e : links) {
                    if (e.text().equals(LINK_SHOW_MORE)){
                        nextURL = LOGIN_PAGE+e.attr("href");
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }

        for (int i=0; i< postsContent.size(); i++) {
            String text = postsContent.get(i);
            String title = text.substring(0, 7);
            String content = text.substring(8, text.length());

            try {
                pStmt = conn.prepareStatement(INSERT_POST);
                pStmt.setString(1, title);
                pStmt.setString(2, content);
                pStmt.executeUpdate();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n\nTotal post stored in DB: " + postsContent.size());
    }

    public static void showPost() {
        System.out.println("History:\n----------");
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SELECT_CONTENT);
            while (rs.next()) {
                System.out.println(rs.getString(TITLE_FIELD)+"\n\t-"+rs.getString(CONTENT_FIELD)+"\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void printCookies(Map<String, String> cookies) {
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            System.out.println("Key: "+entry.getKey()+", Value: "+entry.getValue());
        }
    }

    public static Boolean connectDB() {
//        Connect to db
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        finally {
            return true;
        }

    }
}