/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.mainnet.TransactionValidationParams;
import org.hyperledger.besu.ethereum.transaction.CallParameter;
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator;
import org.hyperledger.besu.ethereum.transaction.TransactionSimulatorResult;
import org.hyperledger.besu.evm.tracing.OperationTracer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Uint256;

/** The Validator contract controller. */
public class ValidatorContractController {
  /** The constant GET_VALIDATORS. */
  public static final String GET_VALIDATORS = "getValidators";
  public static final String SET_NUMBER = "setNumber";
  public static final String GET_NUMBER = "getNumber";

  /** The constant CONTRACT_ERROR_MSG. */
  public static final String CONTRACT_ERROR_MSG = "Failed validator smart contract call";

  private final TransactionSimulator transactionSimulator;
  private final Function getValidatorsFunction;
  private final Function getNumberFunction;
  private final Function setNumberFunction;

  /**
   * Instantiates a new Validator contract controller.
   *
   * @param transactionSimulator the transaction simulator
   */
  public ValidatorContractController(final TransactionSimulator transactionSimulator) {
    this.transactionSimulator = transactionSimulator;

    try {
      this.getValidatorsFunction = new Function(
          GET_VALIDATORS,
          List.of(),
          List.of(new TypeReference<DynamicArray<org.web3j.abi.datatypes.Address>>() {
          }));
      this.getNumberFunction = new Function(
          GET_NUMBER,
          List.of(),
          List.of(new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {
          }));
      this.setNumberFunction = new Function(
          SET_NUMBER,
          List.of(new org.web3j.abi.datatypes.generated.Uint256(0)),
          List.of());
    } catch (final Exception e) {
      throw new RuntimeException("Error creating smart contract function", e);
    }
  }

  /**
   * Gets validators.
   *
   * @param blockNumber     the block number
   * @param contractAddress the contract address
   * @return the validators
   */
  public Collection<Address> getValidators(final long blockNumber, final Address contractAddress) {
    return callFunction(blockNumber, getValidatorsFunction, contractAddress)
        .map(this::parseGetValidatorsResult)
        .orElseThrow(() -> new IllegalStateException(CONTRACT_ERROR_MSG));
  }

  /**
   * Gets the number value from the contract.
   *
   * @param blockNumber     the block number
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
   * @param blockNumber     the block number
   * @param contractAddress the contract address
   * @param value           the value to set
   * @return true if successful, false otherwise
   */
  public boolean setNumber(final long blockNumber, final Address contractAddress, final BigInteger value) {
    return callFunction(blockNumber, setNumberFunction, contractAddress)
        .map(TransactionSimulatorResult::isSuccessful)
        .orElse(false);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Collection<Address> parseGetValidatorsResult(final TransactionSimulatorResult result) {
    final List<Type> resultDecoding = decodeResult(result, getValidatorsFunction);
    final List<org.web3j.abi.datatypes.Address> addresses = (List<org.web3j.abi.datatypes.Address>) resultDecoding
        .get(0).getValue();
    return addresses.stream()
        .map(a -> Address.fromHexString(a.getValue()))
        .collect(Collectors.toList());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private BigInteger parseGetNumberResult(final TransactionSimulatorResult result) {
    final List<Type> resultDecoding = decodeResult(result, getNumberFunction);
    return ((Uint256) resultDecoding.get(0)).getValue();
  }

  private Optional<TransactionSimulatorResult> callFunction(
      final long blockNumber, final Function function, final Address contractAddress) {
    final Bytes payload = Bytes.fromHexString(FunctionEncoder.encode(function));
    final CallParameter callParams = new CallParameter(null, contractAddress, -1, null, null, payload);
    final TransactionValidationParams transactionValidationParams = TransactionValidationParams
        .transactionSimulatorAllowExceedingBalance();
    return transactionSimulator.process(
        callParams, transactionValidationParams, OperationTracer.NO_TRACING, blockNumber);
  }

  @SuppressWarnings("rawtypes")
  private List<Type> decodeResult(
      final TransactionSimulatorResult result, final Function function) {
    if (result.isSuccessful()) {
      final List<Type> decodedList = FunctionReturnDecoder.decode(
          result.result().getOutput().toHexString(), function.getOutputParameters());

      if (decodedList.isEmpty()) {
        throw new IllegalStateException(
            "Unexpected empty result from validator smart contract call");
      }

      return decodedList;
    } else {
      throw new IllegalStateException(
          "Failed validator smart contract call: " + result.getValidationResult());
    }
  }
}
