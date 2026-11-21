-- Multiplayer battle adds friend/classroom modes and a pre-game lobby.
ALTER TYPE "BattleMode" ADD VALUE IF NOT EXISTS 'FRIEND';
ALTER TYPE "BattleMode" ADD VALUE IF NOT EXISTS 'CLASSROOM';
ALTER TYPE "BattleStatus" ADD VALUE IF NOT EXISTS 'LOBBY';
