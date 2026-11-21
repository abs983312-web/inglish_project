import type { WebSocket } from 'ws';

/**
 * In-process realtime hub for battle rooms.
 *
 * v1 runs on a single Fastify instance, so a Map of sockets is enough. If the
 * API is ever scaled horizontally this must move to Redis pub/sub; room state
 * itself already lives in PostgreSQL and is therefore safe to hand over.
 */
export type BattleSocketClient = {
  socket: WebSocket;
  roomId: string;
  participantId: string;
  userId: string;
};

const clientsByRoom = new Map<string, Set<BattleSocketClient>>();
const timersByRoom = new Map<string, NodeJS.Timeout>();
const lockTails = new Map<string, Promise<unknown>>();

export function subscribe(client: BattleSocketClient): void {
  const set = clientsByRoom.get(client.roomId) ?? new Set<BattleSocketClient>();
  set.add(client);
  clientsByRoom.set(client.roomId, set);
}

export function unsubscribe(client: BattleSocketClient): void {
  const set = clientsByRoom.get(client.roomId);
  if (!set) return;
  set.delete(client);
  if (set.size === 0) {
    clientsByRoom.delete(client.roomId);
  }
}

export function onlineParticipantIds(roomId: string): Set<string> {
  const set = clientsByRoom.get(roomId);
  if (!set) return new Set();
  return new Set(
    [...set]
      .filter((client) => client.socket.readyState === 1)
      .map((client) => client.participantId),
  );
}

export function broadcast(roomId: string, payload: unknown): void {
  const set = clientsByRoom.get(roomId);
  if (!set || set.size === 0) return;

  const text = JSON.stringify(payload);
  for (const client of [...set]) {
    if (client.socket.readyState !== 1) {
      set.delete(client);
      continue;
    }
    try {
      client.socket.send(text);
    } catch {
      set.delete(client);
    }
  }

  if (set.size === 0) {
    clientsByRoom.delete(roomId);
  }
}

export function scheduleRoom(roomId: string, at: Date, run: () => Promise<void>): void {
  cancelSchedule(roomId);
  const delay = Math.max(0, at.getTime() - Date.now());
  const timer = setTimeout(() => {
    timersByRoom.delete(roomId);
    void run().catch((error) => {
      // The route layer logs request errors; timers have no request. Keep the
      // process alive and let the next client sync retry the transition.
      // eslint-disable-next-line no-console
      console.error(`[battle] scheduled transition failed for room ${roomId}`, error);
    });
  }, delay);
  timer.unref?.();
  timersByRoom.set(roomId, timer);
}

export function cancelSchedule(roomId: string): void {
  const timer = timersByRoom.get(roomId);
  if (!timer) return;
  clearTimeout(timer);
  timersByRoom.delete(roomId);
}

export function closeRoomSockets(roomId: string): void {
  const set = clientsByRoom.get(roomId);
  if (!set) return;
  for (const client of [...set]) {
    try {
      client.socket.send(JSON.stringify({ type: 'room-closed' }));
      client.socket.close(1000, 'Room closed');
    } catch {
      // ignore
    }
  }
  clientsByRoom.delete(roomId);
}

/**
 * Serialises room mutations inside one Node process. The database transaction
 * is still the source of truth, but the lock prevents the round timer and the
 * last answer from finalising the same round twice.
 */
export async function withRoomLock<T>(roomId: string, run: () => Promise<T>): Promise<T> {
  const previous = lockTails.get(roomId) ?? Promise.resolve();
  const next = previous.catch(() => undefined).then(run);
  lockTails.set(roomId, next);
  try {
    return await next;
  } finally {
    if (lockTails.get(roomId) === next) {
      lockTails.delete(roomId);
    }
  }
}

/** Test helper: forget every socket, timer, and lock. */
export function resetBattleHub(): void {
  for (const timer of timersByRoom.values()) {
    clearTimeout(timer);
  }
  timersByRoom.clear();
  clientsByRoom.clear();
  lockTails.clear();
}
