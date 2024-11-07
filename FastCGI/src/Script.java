public class Script {

    public static String defenitionHit(int x, int y, float r) {

        String str = "Точка не попадает в область";

        if(x >= 0 && x <= r && y >= 0 && y <= (-x + r)){
            str = "Точка попадает в область";
        }
        if (x >= 0 && x <= r/2 && y >= 0 && y <= r/2 && (x*x + y*y <= (r/2) * (r/2))){
            str = "Точка попадает в область";
        }
        if (x >= -r && x <= 0 && y >= -r && y <= -r/2){
            str = "Точка попадает в область";
        }

        return str;
    }

}