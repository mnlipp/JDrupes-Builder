package jdbld.demo.subprojects.app;

import jdbld.demo.subprojects.base1.Base1;

@SuppressWarnings({ "PMD.ShortClassName", "PMD.CommentRequired",
    "PMD.UseUtilityClass" })
public class App {

    public App() {
        new Base1();
    }

    public static void main(String[] args) {
        new App();
    }
}
