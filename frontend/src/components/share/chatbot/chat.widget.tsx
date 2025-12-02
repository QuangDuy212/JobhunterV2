import React, { useState } from "react";
import { MessageCircle, X } from "lucide-react";
import ChatBox from "./chat.box"; // Giả sử ChatBox vẫn sử dụng CSS/Tailwind nội bộ

// Định nghĩa styles dưới dạng đối tượng (Style Inline)
const styles = {
  // Container chính (Tailwind: bottom-6 right-6 z-[9999])
  mainContainer: {
    position: "fixed" as "fixed", // 🚨 Quan trọng: Sửa từ 'absolute' sang 'fixed' cho widget
    bottom: 80,
    right: 24,
    zIndex: 9999,
  },

  // Nút bật/tắt (Tailwind: w-14 h-14 bg-blue-600...)
  button: {
    width: 56,
    height: 56,
    backgroundColor: "#2563EB", // blue-600
    color: "white",
    borderRadius: "9999px", // rounded-full
    boxShadow: "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)", // shadow-lg
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    cursor: "pointer",
    border: "none",
    transition: "all 0.2s",
  },

  // Khung chat (Tailwind: absolute bottom-20 right-0 w-96 h-[500px]...)
  chatFrame: {
    position: "absolute" as "absolute",
    bottom: 80,
    right: 0,
    width: 384, // w-96
    height: 500,
    backgroundColor: "white",
    borderRadius: "1rem", // rounded-2xl
    boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)", // shadow-2xl
    border: "1px solid #E5E7EB", // border-gray-200
    overflow: "hidden" as "hidden",
    display: "flex",
    flexDirection: "column" as "column",
    // animate-fadeIn cần định nghĩa CSS thuần hoặc sử dụng thư viện animate-on-scroll
  },

  // Header chat (Tailwind: bg-blue-600 text-white p-4 font-semibold)
  chatHeader: {
    backgroundColor: "#2563EB", // blue-600
    color: "white",
    padding: 16, // p-4
    fontWeight: "600" as "600",
  },
};

// ⚠️ Lưu ý: Các hover state (hover:bg-blue-700) và animation (animate-fadeIn) 
// không thể được xử lý trực tiếp bằng Style Inline trong React. 
// Chúng ta cần sử dụng state hoặc thư viện CSS-in-JS/SCSS cho các hiệu ứng đó.
// Ở đây, tôi sẽ giả lập hiệu ứng hover bằng một hàm.

export default function ChatWidget() {
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [isHovered, setIsHovered] = useState<boolean>(false);

  // Tạo style động cho hiệu ứng hover
  const dynamicButtonStyle = {
    ...styles.button,
    backgroundColor: isHovered ? "#1D4ED8" : "#2563EB", // blue-700 khi hover
  };

  return (
    <div style={styles.mainContainer}>
      {/* Nút tròn bật/tắt chat */}
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
        style={dynamicButtonStyle}
      >
        {isOpen ? <X size={28} /> : <MessageCircle size={28} />}
      </button>

      {/* Khung chat */}
      {isOpen && (
        <div style={styles.chatFrame}>
          <ChatBox />
        </div>
      )}
    </div>
  );
}