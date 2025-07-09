// src/Peer.js
class Peer {
  constructor(socket, roomId, peerId, displayName) {
    this.socket = socket;
    this.roomId = roomId;
    this.id = peerId;
    this.displayName = displayName;
    this.userId = null; // 사용자 ID
    this.role = 'PARTICIPANT'; // 역할 (HOST/PARTICIPANT)
    this.transports = new Map();
    this.producers = new Map();
    this.consumers = new Map();
    this.isActive = true; // 활성 상태
    this.joinedAt = new Date(); // 참가 시간
    this.leftAt = null; // 나간 시간
    this.rejoinedAt = null; // 재접속 시간
  }

  addTransport(transport) {
    this.transports.set(transport.id, transport);
  }

  getTransport(transportId) {
    return this.transports.get(transportId);
  }

  addProducer(producer) {
    this.producers.set(producer.id, producer);
  }

  getProducer(producerId) {
    return this.producers.get(producerId);
  }

  // 특정 종류(kind)의 producer 찾기
  getProducerByKind(kind, isScreen = false) {
    for (const [id, producer] of this.producers.entries()) {
      // 화면 공유가 아닌 일반 비디오 producer 찾기
      if (producer.kind === kind) {
        const isScreenShare = producer.appData && producer.appData.mediaType === 'screen';
        if ((isScreen && isScreenShare) || (!isScreen && !isScreenShare)) {
          return producer;
        }
      }
    }
    return null;
  }

  addConsumer(consumer) {
    this.consumers.set(consumer.id, consumer);
  }

  getConsumer(consumerId) {
    return this.consumers.get(consumerId);
  }

  close() {
    // 모든 Transport 정리
    this.transports.forEach(transport => transport.close());

    // Socket 연결 종료
    this.socket.disconnect(true);
  }

  toJson() {
      return {
        peerId: this.id,
        userId: this.userId,
        displayName: this.displayName,
        role: this.role,
        isActive: this.isActive,
        joinedAt: this.joinedAt,
        producerCount: this.producers.size,
        consumerCount: this.consumers.size
      };
  }
}

module.exports = Peer;