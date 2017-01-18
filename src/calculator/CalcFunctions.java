package calculator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CalcFunctions {  
  
  /**
   * Class which contains only mathematical functions with no consideration to interface.
   * Functions that return BigIntegers or Strings are to be used directly by CalcInterface,
   * and thus are public. null is returned when the calculation is mathematically invalid
   * or exceeds reasonable computational limits.
   */
  
  private List<List<BigInteger>> setPartitionTable;
  private List<BigInteger> setPartitionList;
  private List<List<BigInteger>> intPartitionTable;
  private List<BigInteger> intPartitionList;
  
  private BigInteger largestChecked;
  private TreeSet<BigInteger> primes;
  private TreeMap<BigInteger, List<BigInteger>> witnesses;
  
  private final BigInteger TWO = BigInteger.valueOf(2);
  private final BigInteger THREE = BigInteger.valueOf(3);
  private final BigInteger FIVE = BigInteger.valueOf(5);
  
  private final BigInteger POWER_LIMIT = new BigInteger("99999");
  private final BigInteger FACTORIZATION_LIMIT = new BigInteger("999999999999");
  private final BigInteger MERSENNE_LIMIT =
      new BigInteger(new String(new char[1000]).replace("\0", "9")); // 1000 digit limit
  private final BigInteger SEQUENCE_LIMIT = new BigInteger("9999"); // Includes factorials
  private final BigInteger PRIME_GEN_LIMIT = new BigInteger("9999999");
  private final BigInteger INT_PARTITION_LIMIT = new BigInteger("999");
  private final BigInteger SET_PARTITION_LIMIT = new BigInteger("600");
  private final BigInteger QUAD_RESIDUE_LIMIT = new BigInteger("99999");
  private final BigInteger ISQRT_LIMIT =
      new BigInteger(new String(new char[2000]).replace("\0", "9")); 
  
  /**
   * All numbers less than the the leftmost integer can be unconditionally confirmed to be prime or
   * composite using the rest of the numbers (witnesses) in the list. Used for the Miller-Rabin
   * primality test. This should never be modified, unless new results of the Miller-Rabin test
   * are definitively proven.
   */
  public final String[] witnessStr = {
      "2047 2",
      "1373653 2 3",
      "9080191 31 73",
      "25326001 2 3 5",
      "3215031751 2 3 5 7",
      "4759123141 2 7 61",
      "1122004669633 2 13 23 1662803",
      "2152302898747 2 3 5 7 11",
      "3474749660383 2 3 5 7 11 13",
      "341550071728321 2 3 5 7 11 13 17",
      "3825123056546413051 2 3 5 7 11 13 17 19 23",
      "318665857834031151167461 2 3 5 7 11 13 17 19 23 29 31 37",
      "999999999999999999999999 2 3 5 7 11 13 17 19 23 29 31 37 41"
      };
  
  public CalcFunctions() {
    setPartitionTable = new ArrayList<List<BigInteger>>();
    setPartitionTable.add(new ArrayList<BigInteger>());
    setPartitionTable.get(0).add(BigInteger.ONE);
    setPartitionList = new ArrayList<BigInteger>();
    setPartitionList.add(BigInteger.ONE);
    
    intPartitionTable = new ArrayList<List<BigInteger>>();
    intPartitionTable.add(new ArrayList<BigInteger>());
    intPartitionTable.get(0).add(BigInteger.ONE);
    intPartitionList = new ArrayList<BigInteger>();
    intPartitionList.add(BigInteger.ONE);
        
    primes = new TreeSet<BigInteger>();
    primes.add(TWO);
    primes.add(THREE);
    primes.add(FIVE);
    largestChecked = FIVE;
    
    witnesses = new TreeMap<BigInteger, List<BigInteger>>();
    readWitnesses();
  }
  
  /**
   * Converts witnessStr into a map, with the upper limit being the keys and a list of witnesses
   * being the value.
   */
  private void readWitnesses() {
    for(String i : witnessStr) {
      String[] nums = i.split(" ");
      if (nums.length < 2)
        continue;
      BigInteger limit = new BigInteger(nums[0]);
      List<BigInteger> witnessList = new LinkedList<BigInteger>();
      witnesses.put(limit, witnessList);
      for (int j = 1; j < nums.length; j++) {
        witnessList.add(new BigInteger(nums[j]));
      }
    }
  }

  /**
   * Wrapper for the BigInteger divide function, to check for division by zero.
   * @param x Dividend
   * @param y Divisor
   * @return x / y (integer division), or null if y is 0
   */
  public BigInteger newDivide(BigInteger x, BigInteger y) {
    if (y.equals(BigInteger.ZERO))
      return null;
    return x.divide(y);
  }

  /**
   * Wrapper for the BigInteger mod function, to check for non-positive moduli.
   * @param x Dividend
   * @param y Divisor
   * @return x mod y if y >= 1 guaranteed to be between 0 and y - 1, null otherwise
   */
  public BigInteger newMod(BigInteger x, BigInteger y) {
    if (y.signum() != 1)
      return null;
    return x.mod(y); 
  }
  
  /**
   * Wrapper for the BigInteger modInverse function, to check for non-positive moduli and to ensure
   * that x and y are coprime (gcd(x, y) = 1)
   * @param x Any integer
   * @param y The modulus
   * @return The solution z to the congruence x * z = 1 (mod y), if gcd(x, y) = 1 and y >= 1,
   * null otherwise 
   */
  public BigInteger newModInverse(BigInteger x, BigInteger y) {
    if (y.signum() != 1)
      return null;
    if (!x.gcd(y).equals(BigInteger.ONE))
      return null;
    return x.modInverse(y);
  }
  
  /**
   * Wrapper for the BigInteger pow function, to check for negative exponents and limit.
   * @param x Base
   * @param y Exponent
   * @return x^y if 0 <= y <= POWER_LIMIT, null otherwise
   */
  public BigInteger newPow(BigInteger x, BigInteger y) {
    if (y.compareTo(POWER_LIMIT) > 0)
      return null;
    if (y.signum() == -1)
      return null;
    int n = y.intValue();
    return x.pow(n);
  }

  /**
   * Computes the largest integer x such that x^2 <= n. Uses Newton's method of quadratic
   * convergence.
   * @param n Number to compute the integer square root of
   * @return The integer square root of n, or null if n is negative or exceeds the integer
   * square root limit
   */
  public BigInteger isqrt(BigInteger n) {
    if (n.signum() != 1 || n.compareTo(ISQRT_LIMIT) > 0)
      return null;
    BigInteger x = n, y = (x.add(n.divide(x)).divide(TWO));
    while (x.compareTo(y) > 0) {
      x = y;
      y = (x.add(n.divide(x)).divide(TWO));
    } 
    return x;
  }

  /**
   * Calculates the smallest positive integer that is divisible by both a and b.
   * lcm(a, b) * gcd(a, b) = a * b
   * @param a Any integer
   * @param b Any integer
   * @return The least common multiple of a and b
   */
  public BigInteger lcm(BigInteger a, BigInteger b) {
    if (a.equals(BigInteger.ZERO) || b.equals(BigInteger.ZERO)) return null;
    return a.multiply(b).divide(a.gcd(b));
  }

  /**
   * Calculates the product of all integers less than or equal to x, by iterative multiplication.
   * @param x The integer to calculate the factorial of
   * @return x!, or null if x < 0 or x > SEQUENCE_LIMIT;
   */
  public BigInteger factorial(BigInteger x) {
    if (x.compareTo(SEQUENCE_LIMIT) > 0)
      return null;
    if (x.compareTo(BigInteger.ZERO) < 0)
      return null;
    BigInteger r = BigInteger.ONE;
    for (BigInteger i = TWO; i.compareTo(x) < 1; i = i.add(BigInteger.ONE)) {
      r = r.multiply(i);
    }
    return r;
  }
  
  /**
   * Calculates the product of all integers with the same parity (odd or even) less than or equal
   * to x, by iterative multiplication. x!! will always be less than or equal to x!.
   * @param x The integer to calculate the double factorial of
   * @return x!!, or null if x < 0 or x > SEQUENCE_LIMIT;
   */
  public BigInteger doubleFactorial(BigInteger x) {
    if (x.compareTo(SEQUENCE_LIMIT) > 0)
      return null;
    if (x.compareTo(BigInteger.ZERO) < 0)
      return null;
    BigInteger r = BigInteger.ONE;
    for (BigInteger i = TWO.add(x.mod(TWO)); i.compareTo(x) < 1; i = i.add(TWO)) {
      r = r.multiply(i);
    }
    return r;
  }
  
  /**
   * Calculates the number permutations of the elements of a set such that no element appears in
   * its original position, using the recursive formula:
   * !n = (n - 1) * (!(n - 1) + !(n - 2))
   * @param x The integer to count the number of derangements of
   * @return !n, or null if x < 0 or x > SEQUENCE_LIMIT
   */
  public BigInteger derangement(BigInteger x) {
    if (x.compareTo(SEQUENCE_LIMIT) > 0)
      return null;
    if (x.compareTo(BigInteger.ZERO) < 0)
      return null;
    // Base cases: !0 = 1 and !1 = 0.
    if (x.equals(BigInteger.ZERO))
      return BigInteger.ONE;
    if (x.equals(BigInteger.ONE))
      return BigInteger.ZERO;
    // r1 indicates the previous value, r2 indicates the value from two previous positions
    BigInteger r = BigInteger.ONE, r1 = BigInteger.ZERO, r2 = BigInteger.ONE;
    for (BigInteger i = TWO; i.compareTo(x) < 1; i = i.add(BigInteger.ONE)) {
      r = (i.subtract(BigInteger.ONE)).multiply(r1.add(r2));
      r2 = r1;
      r1 = r;
    }
    return r;
  }
  
  /**
   * Calculates the number of ordered subsets of size k one can choose from a set of size n.
   * Can be computed by the formula P(n, k) = n!/(n-k)!, or by multiplying iterative from k to n.
   * @param n Size of set
   * @param k Size of ordered subset
   * @return P(n, k), or null if n exceeds the limit or if n or k are negative
   */
  public BigInteger permutation(BigInteger n, BigInteger k) {
    if (n.compareTo(SEQUENCE_LIMIT) > 0)
      return null;
    if (n.signum() == -1 || k.signum() == -1)
      return null;
    if (k.compareTo(n) > 0)
      return BigInteger.ZERO;
    BigInteger r = BigInteger.ONE;
    for (BigInteger i = k; i.compareTo(n) < 1; i = i.add(BigInteger.ONE)) {
      r = r.multiply(i);
    }
    return r;
  }

  /**
   * Calculates the binomial coefficient of n and k, often defined as C(n, k) = n!/(k! * (n - k!)),
   * but can be more efficiently computed using a multiplicative formula. Defines the number of ways
   * to choose k elements from a set of size n. The binomial coefficient of n, k and n, n - k are
   * equivalent.
   * @param n The size of the set
   * @param k The number of elements to choose
   * @return C(n, k), or null if n exceeds the limit or if n or k are negative
   */
  public BigInteger binomialCoefficient(BigInteger n, BigInteger k) {
    if (n.compareTo(SEQUENCE_LIMIT) > 0)
      return null;
    if (n.signum() == -1 || k.signum() == -1)
      return null;
    if (k.compareTo(n) > 0)
      return BigInteger.ZERO;
    BigInteger end = k.min(n.subtract(k));
    BigInteger numer = BigInteger.ONE;
    BigInteger denom = BigInteger.ONE;
    for (BigInteger i = BigInteger.ONE; i.compareTo(end) < 1; i = i.add(BigInteger.ONE)) {
      numer = numer.multiply(n.add(BigInteger.ONE).subtract(i));
      denom = denom.multiply(i);
    }
    return numer.divide(denom);
  }

  /**
   * Calculates 1 / (n + 1) * C(2n, n)
   * @param n Any non-negative integer
   * @return The n-th Catalan number
   */
  public BigInteger catalan(BigInteger n) {
    if (n.compareTo(SEQUENCE_LIMIT) > 0)
      return null;
    if (n.compareTo(BigInteger.ZERO) < 0)
      return null;
    return binomialCoefficient(n.multiply(TWO), n).divide(n.add(BigInteger.ONE));
  }

  /**
   * Counts the number of partitions of a labeled set of size x split into exactly y non-empty
   * subsets. This is defined by the recurrence relation:
   * S(n, k) = k * S(n - 1, k) + S(n - 1, k - 1)
   * Formally called Stirling numbers of the second kind.
   * Because of the computation involved in a two-dimensional recurrence relation, the results are
   * stored in setPartitionTable, where x, y, maps to the indices x - 1, y - 1 of the table.
   * @param x The size of the set
   * @param y The number of non-empty subsets
   * @return S(n, k), or null if x > SET_PARTITION_LIMIT
   */
  public BigInteger setPartition(BigInteger x, BigInteger y) {
    if (x.compareTo(SET_PARTITION_LIMIT) > 0)
      return null;
    if (x.signum() == -1 || y.signum() == -1)
      return BigInteger.ZERO;
    if (y.compareTo(x) > 0)
      return BigInteger.ZERO;
    // Initial condition
    if (x.equals(BigInteger.ZERO) || y.equals(BigInteger.ZERO))
      return BigInteger.ONE;
    int n = x.intValue();
    int k = y.intValue();
    // If some of the table has already been calculated, start at its end
    int row = setPartitionTable.size();
    BigInteger rowSum, cur;
    for (int i = row; i < n; i++) {
      rowSum = TWO;
      setPartitionTable.add(new ArrayList<BigInteger>());
      setPartitionTable.get(i).add(BigInteger.ONE);
      for (int j = 1; j < i; j++) {
        cur = setPartitionTable.get(i-1).get(j).multiply(BigInteger.valueOf(j+1))
          .add(setPartitionTable.get(i-1).get(j-1));
        setPartitionTable.get(i).add(cur);
        rowSum = rowSum.add(cur);
      }
      setPartitionTable.get(i).add(BigInteger.ONE);
      setPartitionList.add(rowSum); // Save the sum of each row in a separate list,
    }                               // which will be used in another function
    return setPartitionTable.get(n-1).get(k-1);
  }
  
  /**
   * Counts the number of partitions of a labeled set of size x split into any number of non-empty
   * subsets. B(n) is defined by the sum of the row of S(n, k) from k = 1 to k = n. Formally called
   * Bell numbers.
   * @param x The size of the set
   * @return B(n), or null if x > SET_PARTITION_LIMIT
   */
  public BigInteger setPartition(BigInteger x) {
    if (x.compareTo(SET_PARTITION_LIMIT) > 0)
      return null;
    if (x.signum() == -1)
      return null;
    if (x.equals(BigInteger.ZERO))
      return BigInteger.ONE;
    int n = x.intValue();
    int row = setPartitionList.size();
    if (n > row) {
      setPartition(x, x);
    }
    return setPartitionList.get(n - 1);
  }
  
  /**
   * Counts the number of partitions of an unlabeled set (i.e. an integer) of size x split into
   * exactly y non-empty subsets. This is defined by the recurrence relation:
   * P(n, k) = P(n - 1, k - 1) + P(n - k, k)
   * Because of the computation involved in a two-dimensional recurrence relation, the results are
   * stored in intPartitionTable, where x, y, maps to the indices x - 1, y - 1 of the table
   * @param x The size of the set
   * @param y The number of non-empty subsets
   * @return P(n, k), or null if x > SET_PARTITION_LIMIT
   */
  public BigInteger intPartition(BigInteger x, BigInteger y) {
    if (x.compareTo(INT_PARTITION_LIMIT) > 0)
      return null;
    if (x.signum() == -1 || y.signum() == -1)
      return BigInteger.ZERO;
    if (y.compareTo(x) > 0)
      return BigInteger.ZERO;
    if (x.equals(BigInteger.ZERO) || y.equals(BigInteger.ZERO))
      return BigInteger.ONE;
    int n = x.intValue();
    int k = y.intValue();
    int row = intPartitionTable.size();
    BigInteger rowSum, cur;
    for (int i = row; i < n; i++) {
      rowSum = TWO;
      intPartitionTable.add(new ArrayList<BigInteger>());
      intPartitionTable.get(i).add(BigInteger.ONE);
      for (int j = 1; j < i; j++) {
        if (2*j >= i) {
          cur = intPartitionTable.get(i-1).get(j-1);
        } else {
          cur = intPartitionTable.get(i-1).get(j-1).add(intPartitionTable.get(i-j-1).get(j));
        }
        intPartitionTable.get(i).add(cur);
        rowSum = rowSum.add(cur);
      }
      intPartitionTable.get(i).add(BigInteger.ONE);
      intPartitionList.add(rowSum);
    }
    return intPartitionTable.get(n-1).get(k-1);
  }
  
  /**
   * Counts the number of partitions of an unlabeled set of size x split into any number of
   * non-empty subsets. P(n) by the sum of the row of P(n, k) from k = 1 to k = n.
   * @param x The size of the set
   * @return P(n), or null if x > SET_PARTITION_LIMIT
   */
  public BigInteger intPartition(BigInteger x) {
    if (x.compareTo(INT_PARTITION_LIMIT) > 0)
      return null;
    if (x.signum() == -1)
      return null;
    if (x.equals(BigInteger.ZERO))
      return BigInteger.ONE;
    int n = x.intValue();
    int row = intPartitionList.size();
    if (n > row) {
      intPartition(x, x);
    }
    return intPartitionList.get(n - 1);
  }
  
  /**
   * Calculates the x-th Fibonacci number, or any similar sequence, given two initial values, using
   * the formula: F(x) = F(x - 1) + F(x - 2). Fibonacci numbers use initial values of 0 and 1,
   * Lucas numbers use initial values of 2 and 1.
   * @param x Any integer
   * @param first The zeroth number in the sequence
   * @param second The first number in the sequence
   * @return The x-th number in the sequence, or null if x < 0 or x > SEQUENCE_LIMIT
   */
  public BigInteger fibonacci(BigInteger x, BigInteger first, BigInteger second) {
    if (x.compareTo(SEQUENCE_LIMIT) > 0)
      return null;
    if (x.signum() == -1)
      return null;
    if (x.equals(BigInteger.ZERO))
      return first;
    if (x.equals(BigInteger.ONE))
      return second;
    int n = x.intValue();
    BigInteger r = first, r1 = second, r2 = first;
    for (int i = 2; i <= n; i++) {
        r = r1.add(r2);
        r2 = r1;
        r1 = r;
    }
    return r;
  }

  /**
   * Calculates the nth s-gonal number, defined by the formula:
   * P(s, n) = ((s - 2) * n * (n - 1) / 2) + n
   * Polygonal numbers are undefined for n < 3. P(4, n) is equivalent to n^2
   * @param s The number of sides of the polygon
   * @param n The length of each side
   * @return P(s, n), or null if s < 3.
   */
  public BigInteger polygon(BigInteger s, BigInteger n) {
    if (s.compareTo(THREE) < 0)
      return null;
    BigInteger r = s.subtract(TWO).multiply(n).multiply(n.subtract(BigInteger.ONE)).divide(TWO)
        .add(n);
    return r;
  }
  
  /**
   * Calculates the nth centered s-gonal number, defined by the formula:
   * PC(s, n) = (s * n * (n - 1) / 2) + 1
   * Centered polygonal numbers are undefined for n < 3.
   * @param s The number of sides of the polygon
   * @param n The length of each side
   * @return PC(s, n), or null if s < 3.
   */
  public BigInteger polygonCentered(BigInteger s, BigInteger n) {
    if (s.compareTo(THREE) < 0)
      return null;
    BigInteger r =
        s.multiply(n).multiply(n.subtract(BigInteger.ONE))
        .divide(TWO).add(BigInteger.ONE);
    return r;
  }
  
  /**
   * Computes the sum of the divisors of n, each raised to the power x. Setting x to 0 returns the
   * number of divisors of n. Note that computing the sum of divisors gives no information about 
   * the value of any of the non-trivial divisors.
   * @param x The power to raise each of the factor to
   * @param n The number to find the divisors of
   * @return The sum of each divisor of n raised to the power x, or null if x is negative or
   * exceeds the power limit or if n exceeds the factorization limit
   */
  public BigInteger sumDivisors(BigInteger x, BigInteger n) {
    if (n.equals(BigInteger.ZERO) || n.equals(BigInteger.ONE)) return BigInteger.ZERO;
    if (x.signum() == -1 || x.compareTo(POWER_LIMIT) > 0) return null;
    n = n.abs();
    if (n.compareTo(FACTORIZATION_LIMIT) > 0) return null;
    Map<BigInteger, BigInteger> factors = factor(n);
    BigInteger r = BigInteger.ONE;
    Iterator<BigInteger> it = factors.keySet().iterator();
    BigInteger p, pSum, numFactors;
    while (it.hasNext()) {
      p = it.next();
      numFactors = factors.get(p);
      pSum = BigInteger.ONE;
      for (BigInteger i = BigInteger.ONE; i.compareTo(numFactors) < 1; i = i.add(BigInteger.ONE)) {
        pSum = pSum.add(newPow(p, i.multiply(x)));
      }
      r = r.multiply(pSum);
    }
    return r;
  }
  
  /**
   * Calculates the number of distinct prime divisors of m. 0 and 1 have no prime divisors.
   * @param m The number to sum the distinct prime divisors over
   * @return The sum of the distinct prime divisors of m, or null if the number exceeds
   * the limit
   */
  public BigInteger littleOmega(BigInteger m) {
    m = m.abs();
    if (m.compareTo(FACTORIZATION_LIMIT) > 0) return null;
    if (m.equals(BigInteger.ZERO) || m.equals(BigInteger.ONE)) return BigInteger.ZERO;
    return BigInteger.valueOf(factor(m).size());
  }
  
  /**
   * Calculates the sum of the powers of each prime divisor of m. 0 and 1 have no prime divisors.
   * @param m The number to sum the prime powers over
   * @return The sum of the prime powers of m, or null if the number exceeds the limit
   */
  public BigInteger bigOmega(BigInteger m) {
    m = m.abs();
    if (m.compareTo(FACTORIZATION_LIMIT) > 0) return null;
    if (m.equals(BigInteger.ZERO) || m.equals(BigInteger.ONE)) return BigInteger.ZERO;
    Map<BigInteger, BigInteger> factors = factor(m);
    BigInteger r = BigInteger.ZERO;
    for (BigInteger i : factors.keySet()) r = r.add(factors.get(i));
    return r;
  }
  
  /**
   * Computes the number of tuples of size k + 1 of positive integers all less than or equal to n
   * and includes n that have a collective gcd of 1. Setting k = 1 computes Euler's totient
   * function.
   * @param a The upper limit
   * @param k The size of the tuple, minus 1
   * @return Jordan's totient function for a and k
   */
  public BigInteger jordanTotient(BigInteger a, BigInteger k) {
    if (a.signum() != 1 || k.signum() != 1)
      return null;
    if (a.compareTo(FACTORIZATION_LIMIT) >= 0 || k.compareTo(POWER_LIMIT) > 0) 
      return null;
    if (a.equals(BigInteger.ZERO))
      return BigInteger.ONE;
    if (primes.contains(a) || isPrime(a).equals(BigInteger.ONE))
      return newPow(a, k).subtract(BigInteger.ONE);
    Set<BigInteger> factors = factor(a).keySet();
    BigInteger numer = newPow(a, k);
    BigInteger denom = BigInteger.ONE;
    for (BigInteger f : factors) {
      numer = numer.multiply(newPow(f, k).subtract(BigInteger.ONE));
      denom = denom.multiply(newPow(f, k));
    }
    return numer.divide(denom);
  }
  
  /**
   * Returns 0 if a number n is not squarefree, i.e. is divisible by the square of a prime,
   * otherwise returns 1 if the number of prime factors of n is even and -1 if odd. Uses trial
   * division, but immediately returns 0 once it re-encounters a prime.
   * @param n The number to compute the Mobius function of
   * @return The mobius function of n
   */
  public BigInteger mobius(BigInteger n) {
    Set<BigInteger> factors = new HashSet<BigInteger>();
    BigInteger modNum = n;
    BigInteger factor = TWO;
    Iterator<BigInteger> knownPrimes = primes.iterator();
    while (!modNum.equals(BigInteger.ONE)) {
      if (modNum.mod(factor).equals(BigInteger.ZERO)) {
        if (!factors.add(factor)) return BigInteger.ZERO;
        modNum = modNum.divide(factor);
      } else if (factor.pow(2).compareTo(modNum) == 1) {
        factors.add(modNum);
        modNum = BigInteger.ONE;
      } else {
        if (knownPrimes.hasNext()) {
          factor = knownPrimes.next();
        } else {
          if (factor.equals(TWO))
            factor = THREE;
          else
            factor = factor.add(TWO);
        }
      }
    }
    return BigInteger.valueOf(((factors.size() % 2) * -2) + 1);
  }
  
  /**
   * Computes the smallest integer m such that a^m = 1 (mod n) for all integers a coprime to n.
   * The Carmichael function for n is equal to Euler's Totient function if n is prime, and in
   * general always divides Euler's Totient function for n.
   * @param n The modulo
   * @return The Carmichael function of n, or null if the modulo is not positive or exceeds the
   * factorization limit.
   */
  public BigInteger carmichael(BigInteger n) {
    if (n.signum() != 1)
      return null;
    if (n.compareTo(FACTORIZATION_LIMIT) > 0)
      return null;
    BigInteger FOUR = BigInteger.valueOf(4);
    if (n.equals(BigInteger.ONE))
      return BigInteger.ONE;
    if (n.equals(FOUR))
      return TWO;
    if (primes.contains(n)|| isPrime(n).equals(BigInteger.ONE))
      return n.subtract(BigInteger.ONE);
    if (n.getLowestSetBit() + 1 == n.bitLength()) return n.divide(FOUR);
    Map<BigInteger, BigInteger> factors = factor(n);
    Set<BigInteger> carmichaelFactors = new HashSet<BigInteger>();
    for (BigInteger i : factors.keySet()) {
      BigInteger pPow = factors.get(i);
      BigInteger carmichaelNumber =
          (i.subtract(BigInteger.ONE)).multiply(newPow(i, pPow.subtract(BigInteger.ONE)));
      carmichaelFactors.add(carmichaelNumber);
    }
    BigInteger r = BigInteger.ONE;
    for (BigInteger i : carmichaelFactors) {
      r = lcm(r, i);
    }
    return r;
  }
  
  /**
   * Determines if a number is prime using a series of primality tests. Returns 2 as prime, returns
   * 0, 1, and multiples of 2 as non-prime, tests Mersenne numbers using the Lucas-Lehmer test,
   * checks to see if the set of pre-computed primes contains x, and finally tests x using the
   * Miller-Rabin test.
   * @param x The number to check the primality of
   * @return true if the number if prime, false if the number is non-prime, or null if the number
   * exceeds the Mersenne limit if it is a Mersenne number or exceeds the 24 digit limit for all
   * other numbers
   */
  public Boolean isPrime(BigInteger x) {
    x = x.abs();
    if (x.equals(TWO))
      return true;
    if (x.equals(BigInteger.ZERO)
        || x.equals(BigInteger.ONE)
        || x.mod(TWO).equals(BigInteger.ZERO))
      return false;
    if (x.bitLength() == x.bitCount()) {
      if (x.compareTo(MERSENNE_LIMIT) > 0)
        return null;
      else
        return (lucasLehmer(x));
    }
    if (x.compareTo(witnesses.lastKey()) >= 0)
      return null;
    if (primes.contains(x))
      return true;
    return (millerRabin(x));
  }

  /**
   * Returns an ordered map of BigIntegers, where the keys are the prime factors and the entries
   * are the prime powers corresponding to the factorization of n. Uses trial division, starting
   * with known primes and extending past them if necessary.
   * @param n The number to factor
   * @return The prime factorization of n as a map of primes to powers
   */
  private TreeMap<BigInteger, BigInteger> factor(BigInteger n) {
    TreeMap<BigInteger, BigInteger> factors = new TreeMap<BigInteger, BigInteger>();
    BigInteger modNum = n;
    BigInteger factor = TWO;
    Iterator<BigInteger> knownPrimes = primes.iterator();
    while (!modNum.equals(BigInteger.ONE)) {
      if (modNum.mod(factor).equals(BigInteger.ZERO)) {
        if (!factors.containsKey(factor)) {
          factors.put(factor, BigInteger.ONE);
        } else {
          factors.put(factor, factors.get(factor).add(BigInteger.ONE));
        }
        modNum = modNum.divide(factor);
      } else if (factor.pow(2).compareTo(modNum) == 1) {
        factors.put(modNum, BigInteger.ONE);
        modNum = BigInteger.ONE;
      } else {
        if (knownPrimes.hasNext()) {
          factor = knownPrimes.next();
        } else {
          if (factor.equals(TWO)) factor = THREE;
          else factor = factor.add(TWO);
        }
      }
    }
    return factors;
  }

  /**
   * Determines primality of a integer using the Miller-Rabin test. This test is usually
   * probabilistic, but this algorithm uses a deterministic variant up to 24 digits via the
   * witnesses map.
   * @param n The number to test the primality of
   * @return true if the number is prime and has fewer than 24 digits, false otherwise
   */
  private boolean millerRabin(BigInteger n) {
    // nM = n - 1, nM = 2^r * d,
    // a = witness to test, x = result of the test
    // witnessCeiling = smallest value greater than n that has a known witness list
    BigInteger nM, d, witnessCeiling;
    nM = n.subtract(BigInteger.ONE);
    int r = n.getLowestSetBit();
    d = nM.shiftRight(r);
    witnessCeiling = witnesses.higherKey(n);
    boolean con = false;
    if (witnessCeiling != null) {
      List<BigInteger> nWitnesses = witnesses.get(witnessCeiling);
      BigInteger x;
      for (BigInteger a : nWitnesses) {
        x = a.modPow(d, n);
        if (x.equals(BigInteger.ONE) || x.equals(n.subtract(BigInteger.ONE))) continue;
        for (int j = 0; j < r - 1; j++) {
          x = x.modPow(TWO, n);
          if (x.equals(BigInteger.ONE))
            return false;
          if (x.equals(n.subtract(BigInteger.ONE))) {
            con = true;
            break;
          }
        }
        if (!con)
          return false;
      }
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Determines primality of a Mersenne number, i.e. a number of the form 2^n - 1, using the
   * deterministic Lucas-Lehmer primality test. This is a faster test than Miller-Rabin, and thus
   * supports computing higher numbers.
   * @param n The Mersenne number to determine the primality of
   * @return true if the Mersenne number is prime, false if the number is composite, not a Mersenne
   * number, or exceeds the limit
   */
  private boolean lucasLehmer(BigInteger n) {
    if (n.compareTo(MERSENNE_LIMIT) > 0)
      return false;
    if (n.bitLength() != n.bitCount())
      return false;
    if (n.equals(THREE)) return true;
    BigInteger s = BigInteger.valueOf(4);
    int p = n.bitCount();
    // If the n in 2^n - 1 is composite, 2^n - 1 is composite
    if (isPrime(BigInteger.valueOf(p)).equals(BigInteger.ZERO)) return false;
    for (int i = 0; i < p - 2; i++) {
      s = (s.pow(2).subtract(TWO).mod(n));
    }
    if (s.equals(BigInteger.ZERO))
      return true;
    else
      return false;
  }

  /**
   * Generates all primes below some biglimit using the Sieve of Atkin, then returns the number of
   * primes less than or equal to n.
   * Primes found during this process are added to the set primes.
   * @param bigLimit
   * @return
   */
  public BigInteger sieveOfAtkin(BigInteger bigLimit) {
    if (bigLimit.compareTo(PRIME_GEN_LIMIT) > 0) return null;
    if (bigLimit.signum() == -1) return bigLimit;
    if (bigLimit.compareTo(largestChecked) <= 0) {
      return BigInteger.valueOf(primes.headSet(bigLimit, true).size());
    }
    largestChecked = bigLimit;
    int limit = bigLimit.intValue();
    BitSet sieve = new BitSet(limit);
    int xSquared, ySquared, n, nM;
    // 4x^2 + y^2 = n
    for (int x = 1; x*x < limit; x++) {
      xSquared = x*x;
      for (int y = 1; y*y < limit; y += 2) {
        ySquared = y*y;
        n = (4*xSquared) + (ySquared);
        if (n <= limit) {
          nM = n % 60;
          if (nM == 1 || nM == 13 || nM == 17 ||
            nM == 29 || nM == 37 || nM == 41 ||
            nM == 49 || nM == 53) sieve.flip(n);
        }
      }
    }
    // 3x^2 + y^2 = n
    for (int x = 1; x*x < limit; x += 2) {
      xSquared = x*x;
      for (int y = 2; y*y < limit; y += 2) {
        ySquared = y*y;
        n = (3*xSquared) + (ySquared);
        if (n <= limit) {
          nM = n % 60;
          if (nM == 7 || nM == 19 || nM == 31 || nM == 43) sieve.flip(n);
        }
      }
    }
    // 3x^2 - y^2 = n
    for (int x = 1; x*x < limit; x ++) {
      xSquared = x*x;
      for (int y = (x % 2) + 1; y*y < limit; y += 2) {
        if (x > y) {
          ySquared = y*y;
          n = (3*xSquared) - (ySquared);
          if (n <= limit) {
            nM = n % 60;
            if (nM == 11 || nM == 23 ||
              nM == 47 || nM == 59) {
              sieve.flip(n);
            }
          }
        }
      }
    }   
    for (int r = 7; r * r < limit; r++) {
      if (sieve.get(r)) {
        for (int i = r * r; i < limit; i += 2*r*r) {
          sieve.clear(i);
        }
      }
    }
    for (int a = 7; a < limit; a++) {
      if (sieve.get(a)) {
        primes.add(BigInteger.valueOf(a));
      }
    }
    return BigInteger.valueOf(primes.size());
  }
  
  /**
   * Computes the product of all primes less than or equal to a non-negative n. If all the primes
   * below n have not yet been computed, sieveOfAtkin is called.
   * @param n The number to find the primorial of
   * @return The primorial of n, or null if n is negative or exceeds the limit
   */
  public BigInteger primorial(BigInteger n) {
    if (n.compareTo(SEQUENCE_LIMIT) > 0)
      return null;
    if (n.signum() == -1)
      return null;
    if (n.compareTo(largestChecked) > 0)
      sieveOfAtkin(n);
    BigInteger r = BigInteger.ONE;
    for (BigInteger i : primes.headSet(n, true)) {
      r = r.multiply(i);
    }
    return r;
  }
  
  /**
   * Returns 0 if a is a multiple of m, 1 is a is a quadratic residue of m, -1 if a is not a
   * quadratic residue of m. Only defined for odd values of m. 
   * @param a Number to check if a quadratic residue exists for 
   * @param m Modulus
   * @return The Jacobi symbol for a and m, or null if m is odd.
   */
  public BigInteger jacobi(BigInteger a, BigInteger m) {
    if (m.mod(TWO).equals(BigInteger.ZERO)) return null;
    if (a.equals(BigInteger.ZERO)) {
      if (m.equals(BigInteger.ONE)) return BigInteger.ONE;
      else return BigInteger.ZERO;
    }
    BigInteger r = BigInteger.ONE;
    BigInteger FOUR = BigInteger.valueOf(4), EIGHT = BigInteger.valueOf(8), temp;
    while (true) {
      a = a.mod(m);
      int powerOfTwo = a.getLowestSetBit();
      if (m.mod(EIGHT).equals(THREE) || m.mod(EIGHT).equals(FIVE)) {
        r = r.multiply(BigInteger.ONE.negate().pow(powerOfTwo));
      }
      a = a.divide(TWO.pow(powerOfTwo));
      if (a.equals(BigInteger.ONE)) return r;
      if (!a.gcd(m).equals(BigInteger.ONE)) return BigInteger.ZERO;
      if (a.mod(FOUR).equals(THREE) && m.mod(FOUR).equals(THREE)) r = r.negate();
      temp = a;
      a = m;
      m = temp;
    }
  }

  /**
   * Computes the set of quadratic residues mod m, that is, r is a quadratic residue mod m if there
   * exists an integer such that x^2 = r (mod m). 0 is a trivial residue and is excluded from the
   * result set.
   * @param m Modulo
   * @return The set of quadratic residues mod m, or null if m <= 0
   */
  private TreeSet<BigInteger> quadResidue(BigInteger m) {
    if (m.signum() != 1) return null;
    if (m.compareTo(QUAD_RESIDUE_LIMIT) > 0) return null;
    TreeSet<BigInteger> r = new TreeSet<BigInteger>();
    // Symmetrically, one needs only to check until m/2 for residues
    for (BigInteger i = BigInteger.ONE; i.compareTo(m.divide(TWO)) < 1; i = i.add(BigInteger.ONE)) {
      r.add(i.modPow(TWO, m));
    }
    return r;
  }

  /**
   * Converts the result of isPrime to a string of "Prime", "Composite", or "N/A"
   * @param x The number to check the primality of
   * @return "Prime" or "Composite" if the number if prime or composite, "N/A" if
   * the number is 0 or 1, or null if x exceeds some limit
   */
  public String stringifyPrime(BigInteger x) {
    if (x.equals(BigInteger.ZERO) || x.equals(BigInteger.ONE))
      return "N/A";
    Boolean result = isPrime(x);
    if (result == null)
      return null;
    if (result)
      return "Prime";
    else return "Composite";
  }

  /**
   * Presents the results of the factor method as a prime factorization string, e.g. 2^2 * 3 * 5^3.
   * @param n The integer to factor
   * @return The prime factorization of n as a string
   */
  public String stringifyFactors(BigInteger n) {
    n = n.abs();
    if (n.compareTo(BigInteger.ONE) <= 0) return null;
    if (n.compareTo(FACTORIZATION_LIMIT) > 0) return null;
    String r = "";
    Map<BigInteger, BigInteger> factors = factor(n);
    Iterator<BigInteger> it = factors.keySet().iterator();
    while(it.hasNext()) {
      BigInteger e = it.next();
      r += e + (factors.get(e) == BigInteger.ONE ? "" : "^" + factors.get(e))
        + (it.hasNext() ? " \u00D7 " : "");
    }
    return r;
  }

  /**
   * Presents the results of the factor method as a list of divisors. The number of divisors a
   * number has is equal to the product of every prime power plus one. This method enumerates these
   * divisors through recursion.
   * @param n The number to find the divisors of
   * @return The list of divisors as a string
   */
  public String stringifyDivisors(BigInteger n) {
    n = n.abs();
    if (n.compareTo(BigInteger.ONE) <= 0) return null;
    if (n.compareTo(FACTORIZATION_LIMIT) > 0) return null;
    TreeMap<BigInteger, BigInteger> factors = factor(n);
    TreeSet<BigInteger> divisors = new TreeSet<BigInteger>();
    divisorsRecurse(factors.firstKey(), factors, divisors, BigInteger.ONE);
    return divisors.toString();
  }

  public String stringifyQuadResidue(BigInteger n) {
    if (n.signum() != 1) return null;
    if (n.compareTo(QUAD_RESIDUE_LIMIT) > 0) return null;
    return quadResidue(n).toString();
  }

  /**
   * Recursive helper function to find divisors of a number
   * @param curEl The current prime factor being considered, immutable
   * @param factors The mapping of all prime factors to their powers, immutable
   * @param divisors The current set of divisors
   * @param total The current product of the previous prime factors to some power
   */
  public void divisorsRecurse(final BigInteger curEl, final TreeMap<BigInteger, BigInteger> factors,
      TreeSet<BigInteger> divisors, BigInteger total) {
    if (curEl == null) {
      // When at the end of the prime factors list, stop, and add product to divisors list
      divisors.add(total); 
    } else {
      BigInteger nextEl = factors.higherKey(curEl);
      BigInteger primePow = factors.get(curEl);
      // Iterate through every possible prime power for the current element
      for (BigInteger i = BigInteger.ZERO; i.compareTo(primePow) <= 0; i = i.add(BigInteger.ONE)) {
        divisorsRecurse(nextEl, factors, divisors, newPow(curEl, i).multiply(total));
      }
    }
  }
}
