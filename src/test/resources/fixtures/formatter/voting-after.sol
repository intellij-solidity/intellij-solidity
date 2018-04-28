pragma solidity ^0.4.23;
// We have to specify what version of compiler this code will compile with

contract Voting {
    /* mapping field below is equivalent to an associative array or hash.
    The key of the mapping is the candidate name stored as type bytes32 and value is
    an unsigned integer to store the vote count
    */

    mapping(bytes32 => uint8) public votesReceived;

    /* Solidity doesn't let you pass in an array of strings in the constructor (yet).
    We will use an array of bytes32 instead to store the list of candidates
    */

    bytes32[] public candidateList;

    /* This is the constructor which will be called once when you
    deploy the contract to the blockchain. When we deploy the contract,
    we will pass an array of candidates who will be contesting in the election
    */
    constructor(bytes32[] candidateNames) public {
        candidateList = candidateNames;
    }

    /* Accessing class attributes directly with web3py or web3js sometimes leads to
    unpredictable behavior. To be safe we create a getter method that returns our
    array of candidate names.
    */
    function getCandidateList() public view returns (bytes32[]) {
        return candidateList;
    }

    // This function returns the total votes a candidate has received so far
    function totalVotesFor(bytes32 candidate) public view returns (uint8) {
        require(validCandidate(candidate) == true);
        return votesReceived[candidate];
    }

    // This function increments the vote count for the specified candidate. This
    // is equivalent to casting a vote
    function voteForCandidate(bytes32 candidate) public {
        require(validCandidate(candidate) == true);
        votesReceived[candidate] += 1;
    }

    function validCandidate(bytes32 candidate) public view returns (bool) {
        for (uint i = 0; i < candidateList.length; i++) {
            if (candidateList[i] == candidate) {
                return true;
            }
        }
        return false;
    }
}
