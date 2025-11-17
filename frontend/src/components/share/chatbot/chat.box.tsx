import React, { useState, useEffect, useRef } from "react";
import useWebSocket, { ReadyState } from "react-use-websocket";

interface ChatMessage {
  sender: "user" | "bot" | "error";
  text: string;
}

// Định nghĩa các Style Object
const styles = {
  // Container chính (Tailwind: flex flex-col flex-1 p-3 bg-gray-50)
  // 🚨 QUAN TRỌNG: Cần đảm bảo component này nhận được chiều cao từ component cha (ChatWidget)
  mainContainer: {
    display: "flex",
    flexDirection: "column" as "column",
    flex: 1, // Chiếm toàn bộ không gian còn lại
    backgroundColor: "#F9FAFB", // gray-50
    padding: 0, // Bỏ padding ở đây để thêm padding vào messageArea
    height: '100%', // Đảm bảo chiếm hết chiều cao của ChatWidget
  },

  // 🚨 STYLE CHO HEADER MỚI ĐƯỢC THÊM
  chatHeader: {
    backgroundColor: "#2563EB", // blue-600
    color: "white",
    padding: 16, // p-4
    fontWeight: "600" as "600",
    fontSize: 18,
    borderBottom: '1px solid #1E40AF', // blue-700
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    cursor: 'default',
  },
  
  // Vùng hiển thị tin nhắn (Tailwind: flex-1 overflow-y-auto mb-3)
  messageArea: {
    flex: 1,
    overflowY: "auto" as "auto",
    padding: 12, // p-3 (12px)
    // Loại bỏ marginBottom vì đã có đường kẻ chia
  },

  // Khung tin nhắn
  messageWrapper: {
    marginBottom: 10, // Giảm khoảng cách giữa các tin nhắn một chút
    display: 'flex', 
  },

  // Tin nhắn bot đang trả lời (animate-pulse bị loại bỏ)
  typingIndicator: {
    display: "inline-block",
    padding: "8px 12px", // px-3 py-2
    borderRadius: 8, // rounded-lg
    backgroundColor: "#E5E7EB", // gray-200
    color: "#6B7280", // gray-500
    fontStyle: 'italic',
  },

  // Vùng nhập liệu (có đường kẻ chia)
  inputArea: {
    display: "flex",
    borderTop: "1px solid #E5E7EB", // gray-200
    padding: 10, // Khoảng cách xung quanh thanh nhập liệu
    backgroundColor: "white",
  },

  // Input (Tailwind: flex-1 border border-gray-300 p-2 rounded-l-lg outline-none)
  inputField: {
    flex: 1,
    border: "1px solid #D1D5DB", // gray-300
    padding: 8, // p-2
    borderRadius: 8, // rounded-lg cho cả 2 bên
    outline: "none",
    fontSize: "1rem",
    marginRight: 8, // Khoảng cách giữa input và button
  },

  // Nút gửi (Base styles)
  sendButtonBase: {
    padding: "8px 16px", // px-4
    border: "none",
    borderRadius: 8,
    transition: "background-color 0.2s",
    cursor: "pointer",
    fontSize: "1rem",
    fontWeight: "600",
  },

  // Trạng thái kết nối
  connectionStatus: {
    textAlign: "center" as "center",
    marginTop: 8, // mt-2
    fontSize: 14, // text-sm
    color: "#EF4444", // red-500
    padding: '0 12px 12px 12px',
  },
};

// Định nghĩa hàm trả về style cho từng loại tin nhắn
const getMessageBubbleStyle = (sender: ChatMessage["sender"]) => {
  let baseStyle: React.CSSProperties = {
    display: "inline-block",
    padding: "10px 16px", 
    borderRadius: 12, 
    maxWidth: "85%", // Mở rộng hơn một chút
    wordBreak: "break-word",
    whiteSpace: "pre-wrap" as "pre-wrap", 
    lineHeight: 1.5, 
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)', // Thêm shadow nhẹ
  };

  if (sender === "user") {
    // User
    baseStyle.backgroundColor = "#2563EB"; // blue-600
    baseStyle.color = "white";
    baseStyle.borderRadius = "12px 12px 0px 12px"; 
  } else if (sender === "error") {
    // Lỗi
    baseStyle.backgroundColor = "#FEE2E2"; // red-100
    baseStyle.color = "#991B1C"; // red-700
    baseStyle.fontWeight = "600";
    baseStyle.borderRadius = "12px 12px 12px 0px"; 
    baseStyle.border = "1px solid #FCA5A5"; // red-300
  } else {
    // Bot
    baseStyle.backgroundColor = "white"; // Đổi sang nền trắng cho bot
    baseStyle.color = "black";
    baseStyle.borderRadius = "12px 12px 12px 0px"; 
  }
  
  return baseStyle;
};

export default function ChatBox() {
  const [input, setInput] = useState<string>("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isSending, setIsSending] = useState<boolean>(false);
  const [isButtonHovered, setIsButtonHovered] = useState<boolean>(false);
  const messagesEndRef = useRef<HTMLDivElement>(null); // Ref cho auto-scroll

  const { sendJsonMessage, lastJsonMessage, readyState } = useWebSocket<{ message: string }>(
    "ws://localhost:8080/ws/chat",
    {
      onOpen: () => console.log("✅ Connected to chatbot"),
      shouldReconnect: () => true,
    }
  );

  // Auto-scroll
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  // Nhận tin nhắn từ server
  useEffect(() => {
    if (lastJsonMessage) {
      setIsSending(false);

      const botResponseText = lastJsonMessage.message;
      let senderType: "bot" | "error" = "bot";

      if (botResponseText.toLowerCase().includes("lỗi gọi api") || botResponseText.toLowerCase().includes("lỗi từ gemini")) {
        senderType = "error";
      }

      setMessages((prev) => [...prev, { sender: senderType, text: botResponseText }]);
    }
  }, [lastJsonMessage]);

  const sendMessage = () => {
    if (!input.trim() || readyState !== ReadyState.OPEN || isSending) return;

    const userMessage: ChatMessage = { sender: "user", text: input };
    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setIsSending(true);

    sendJsonMessage({ message: userMessage.text });

    setTimeout(() => setIsSending(false), 10000);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") sendMessage();
  };

  // Tạo style động cho nút gửi
  const isButtonDisabled = isSending || readyState !== ReadyState.OPEN;
  
  let sendButtonDynamicStyle: React.CSSProperties;

  if (isButtonDisabled) {
    // Disabled state
    sendButtonDynamicStyle = {
      ...styles.sendButtonBase,
      backgroundColor: "#D1D5DB", // gray-300
      color: "white",
      cursor: "not-allowed",
    };
  } else {
    // Active state
    sendButtonDynamicStyle = {
      ...styles.sendButtonBase,
      backgroundColor: isButtonHovered ? "#1D4ED8" : "#2563EB", // blue-700/blue-600
      color: "white",
    };
  }


  return (
    <div style={styles.mainContainer}>
      {/* 🚨 HEADER CHAT ĐƯỢC THÊM VÀO ĐÂY */}
      <div style={styles.chatHeader}>
        Chat Bot 🤖
      </div>

      {/* Vùng hiển thị chat */}
      <div style={styles.messageArea}>
        {messages.map((msg, i) => (
          <div 
            key={i} 
            style={{
              ...styles.messageWrapper,
              // căn tin nhắn sang phải nếu là user, trái nếu là bot/error
              justifyContent: msg.sender === "user" ? "flex-end" : "flex-start",
              textAlign: msg.sender === "user" ? "right" : "left",
            }}
          >
            <span
              style={getMessageBubbleStyle(msg.sender)}
            >
              {msg.text}
            </span>
          </div>
        ))}

        {/* Trạng thái bot đang gõ */}
        {isSending && (
          <div style={{ ...styles.messageWrapper, justifyContent: "flex-start", textAlign: "left" }}>
            <span style={styles.typingIndicator}>
              Bot đang trả lời...
            </span>
          </div>
        )}
        {/* Điểm neo cho auto-scroll */}
        <div ref={messagesEndRef} /> 
      </div>

      {/* Ô nhập + nút gửi */}
      <div style={styles.inputArea}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          style={styles.inputField}
          placeholder="Enter..."
          disabled={isButtonDisabled}
        />
        <button
          onClick={sendMessage}
          disabled={isButtonDisabled}
          style={sendButtonDynamicStyle}
          onMouseEnter={() => setIsButtonHovered(true)}
          onMouseLeave={() => setIsButtonHovered(false)}
        >
          {isSending ? "Sending..." : "Send"}
        </button>
      </div>

      {/* Trạng thái kết nối */}
      {readyState !== ReadyState.OPEN && (
        <div style={styles.connectionStatus}>
          The WebSocket connection is closed or waiting to reconnect (State: {ReadyState[readyState]}).
        </div>
      )}
    </div>
  );
}