package compression.coding.bigdecimal;

public class Experiment {

    public static void main(String[] args) {
        // Trying out ExactArithmeticEncoder
        //// zoomIntoSubinterval()
        ExactArithmeticEncoder ac = new ExactArithmeticEncoder();
        BigDecimalInterval rule = new BigDecimalInterval(0, 1);
        ac.encodeNext(new BigDecimalInterval(0,0.65));
        ac.encodeNext(new BigDecimalInterval(0.40,0.10));
        ac.encodeNext(new BigDecimalInterval(0.0,0.65));
        ac.encodeNext(new BigDecimalInterval(0.50,0.10));
        ac.encodeNext(new BigDecimalInterval(0.65,0.35));
        ac.encodeNext(new BigDecimalInterval(0.65,0.35));
        System.out.println(ac.getInterval());
        System.out.println(ac.getFinalEncoding());
    }

}
