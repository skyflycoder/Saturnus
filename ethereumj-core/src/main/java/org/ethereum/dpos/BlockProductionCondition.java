package org.ethereum.dpos;

public enum  BlockProductionCondition {
  PRODUCED,           // Successfully generated block
  UNELECTED,
  NOT_MY_TURN,        // It isn't my turn
  NOT_SYNCED,
  NOT_TIME_YET,       // Not yet arrived
  NO_PRIVATE_KEY,
  LOW_PARTICIPATION,
  LAG,
  CONSECUTIVE,
  TIME_OUT,
  BACKUP_STATUS_IS_NOT_MASTER,
  EXCEPTION_PRODUCING_BLOCK
}
