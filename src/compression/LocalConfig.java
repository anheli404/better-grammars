package compression;

import compression.coding.ArithmeticCodingFactory;

/**
 * @author Sebastian Wild (wild@uwaterloo.ca)
 */
public class LocalConfig {

	// TODO Change this to your local path of the git repository clone
	public static final String GIT_ROOT = ".";
	public static final ArithmeticCodingFactory.Backend AC_BACKEND =
			ArithmeticCodingFactory.Backend.BIG_DECIMAL;


}
