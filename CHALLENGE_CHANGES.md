## 1 Modified Validators contract

```solidity
  function getNumber() public view returns (uint256) {
        return number;
    }

    function setNumber(uint256 _num) public {
        number = _num;
    }
```

## 2. Extended ValidatorContractController Class

```java
// Added constants for new functions
public static final String SET_NUMBER = "setNumber";
public static final String GET_NUMBER = "getNumber";

// Added field for getNumber function
private final Function getNumberFunction;

// Modified constructor to initialize getNumberFunction
public ValidatorContractController(final TransactionSimulator transactionSimulator) {
    this.transactionSimulator = transactionSimulator;

    try {
        this.getValidatorsFunction =
            new Function(
                GET_VALIDATORS,
                List.of(),
                List.of(new TypeReference<DynamicArray<org.web3j.abi.datatypes.Address>>() {}));

        // Added initialization for getNumberFunction
        this.getNumberFunction =
            new Function(
                GET_NUMBER,
                List.of(),
                List.of(new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {})
            );
    } catch (final Exception e) {
        throw new RuntimeException("Error creating smart contract function", e);
    }
}

/**
 * Gets the number value from the contract.
 *
 * @param blockNumber the block number
 * @param contractAddress the contract address
 * @return the number value
 */
public BigInteger getNumber(final long blockNumber, final Address contractAddress) {
    return callFunction(blockNumber, getNumberFunction, contractAddress)
        .map(this::parseGetNumberResult)
        .orElseThrow(() -> new IllegalStateException(CONTRACT_ERROR_MSG));
}

/**
 * Sets the number value in the contract.
 *
 * @param blockNumber the block number
 * @param contractAddress the contract address
 * @param value the value to set
 * @return true if successful, false otherwise
 */
public boolean setNumber(final long blockNumber, final Address contractAddress, final BigInteger value) {
    Function setNumberWithValue = new Function(
        SET_NUMBER,
        List.of(new org.web3j.abi.datatypes.generated.Uint256(value)),
        List.of()
    );

    return callFunction(blockNumber, setNumberWithValue, contractAddress)
        .map(TransactionSimulatorResult::isSuccessful)
        .orElse(false);
}

// Added helper method to parse getNumber result
private BigInteger parseGetNumberResult(final TransactionSimulatorResult result) {
    final List<Type> resultDecoding = decodeResult(result, getNumberFunction);
    return ((org.web3j.abi.datatypes.generated.Uint256) resultDecoding.get(0)).getValue();
}
```

## 3. Added Unit Tests

```java
// Added test constant
private static final BigInteger TEST_NUMBER = BigInteger.valueOf(42);

@Test
public void decodesGetNumberResultFromContractCall() {
    // Create expected output for getNumber function
    final String getNumberFunctionResult =
        "000000000000000000000000000000000000000000000000000000000000002a"; // Hex for 42

    // Create the function call parameter
    final Function getNumberFunction =
        new Function(
            ValidatorContractController.GET_NUMBER,
            List.of(),
            List.of(new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {})
        );
    final Bytes numberPayload = Bytes.fromHexString(FunctionEncoder.encode(getNumberFunction));
    final CallParameter numberCallParameter =
        new CallParameter(null, CONTRACT_ADDRESS, -1, null, null, numberPayload);

    // Mock the transaction simulator result
    final TransactionSimulatorResult result =
        new TransactionSimulatorResult(
            transaction,
            TransactionProcessingResult.successful(
                List.of(),
                0,
                0,
                Bytes.fromHexString(getNumberFunctionResult),
                ValidationResult.valid()));

    when(transactionSimulator.process(
        numberCallParameter,
        ALLOW_EXCEEDING_BALANCE_VALIDATION_PARAMS,
        OperationTracer.NO_TRACING,
        1))
        .thenReturn(Optional.of(result));

    // Create the controller and call the method
    final ValidatorContractController validatorContractController =
        new ValidatorContractController(transactionSimulator);

    final BigInteger number = validatorContractController.getNumber(1, CONTRACT_ADDRESS);

    // Verify the result
    assertThat(number).isEqualTo(TEST_NUMBER);
}

@Test
public void setNumberReturnsTrueWhenSuccessful() {
    // Create the controller
    final ValidatorContractController validatorContractController =
        new ValidatorContractController(transactionSimulator);

    // Use Mockito's any() matchers to be more flexible with the parameters
    when(transactionSimulator.process(
        Mockito.any(CallParameter.class),
        Mockito.eq(ALLOW_EXCEEDING_BALANCE_VALIDATION_PARAMS),
        Mockito.eq(OperationTracer.NO_TRACING),
        Mockito.eq(1L)))
        .thenReturn(Optional.of(new TransactionSimulatorResult(
            transaction,
            TransactionProcessingResult.successful(
                List.of(),
                0,
                0,
                Bytes.EMPTY,
                ValidationResult.valid()))));

    // Call the method
    final boolean success = validatorContractController.setNumber(1, CONTRACT_ADDRESS, TEST_NUMBER);

    // Verify the result
    assertThat(success).isTrue();
}

@Test
public void setNumberReturnsFalseWhenUnsuccessful() {
    // Create the controller
    final ValidatorContractController validatorContractController =
        new ValidatorContractController(transactionSimulator);

    // Use Mockito's any() matchers to be more flexible with the parameters
    when(transactionSimulator.process(
        Mockito.any(CallParameter.class),
        Mockito.eq(ALLOW_EXCEEDING_BALANCE_VALIDATION_PARAMS),
        Mockito.eq(OperationTracer.NO_TRACING),
        Mockito.eq(1L)))
        .thenReturn(Optional.of(new TransactionSimulatorResult(
            transaction,
            TransactionProcessingResult.failed(
                0,
                0,
                ValidationResult.invalid(TransactionInvalidReason.INTERNAL_ERROR),
                Optional.empty(),
                Optional.empty()))));

    // Call the method
    final boolean success = validatorContractController.setNumber(1, CONTRACT_ADDRESS, TEST_NUMBER);

    // Verify the result
    assertThat(success).isFalse();
}
```

## 4. Summary of Changes

#### Added a getter and setter to Validators contract

### Added to ValidatorContractController:

**Constants:** `SET_NUMBER` and `GET_NUMBER` for function names  
**Fields:** `getNumberFunction` to store the function definition  
**Methods:**  
`getNumber()`: Retrieves the number value from the contract  
`setNumber()`: Sets the number value in the contract  
`parseGetNumberResult()`: Helper method to parse the result from getNumber

### Added to ValidatorContractControllerTest:

**Constants:** `TEST_NUMBER` with value `42` for testing  
**Test Methods:**  
`decodesGetNumberResultFromContractCall()`: Tests that getNumber correctly decodes the result  
`setNumberReturnsTrueWhenSuccessful()`: Tests that setNumber returns true on success  
`setNumberReturnsFalseWhenUnsuccessful()`: Tests that setNumber returns false on failure  
\*\*

#### Key Features:

**Error Handling:** Consistent with existing code patterns  
**Type Safety:** Proper use of Java generics and Web3j types  
**Testing:** Comprehensive test coverage for both success and failure cases  
**Documentation:** JavaDoc comments for public methods

## 4. Testing

All tests are passing, confirming that the implementation works as expected.

Command to run tests
`./gradlew :consensus:qbft:test --tests "org.hyperledger.besu.consensus.qbft.validator.ValidatorContractControllerTest"`
