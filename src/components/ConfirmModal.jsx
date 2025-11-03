import { AlertCircle, X } from "lucide-react";

export default function ConfirmModal({ 
  message, 
  onConfirm, 
  onCancel, 
  confirmText = "OK", 
  cancelText = "Cancel",
  type = "warning" // warning, danger, info
}) {
  const bgColors = {
    warning: "bg-yellow-50 border-yellow-200",
    danger: "bg-red-50 border-red-200",
    info: "bg-blue-50 border-blue-200",
  };

  const iconColors = {
    warning: "text-yellow-500",
    danger: "text-red-500",
    info: "text-blue-500",
  };

  const buttonColors = {
    warning: "bg-yellow-500 hover:bg-yellow-600",
    danger: "bg-red-500 hover:bg-red-600",
    info: "bg-blue-500 hover:bg-blue-600",
  };

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black bg-opacity-50 backdrop-blur-sm">
      <div className="animate-fadeIn">
        <div
          className={`${bgColors[type]} border-2 rounded-lg shadow-2xl p-6 min-w-[400px] max-w-[500px] transform transition-all duration-300`}
        >
          <div className="flex items-start gap-4 mb-6">
            <div className="flex-shrink-0">
              <AlertCircle className={`w-8 h-8 ${iconColors[type]}`} />
            </div>
            <div className="flex-1">
              <p className="text-gray-800 font-medium text-lg leading-relaxed">
                {message}
              </p>
            </div>
            <button
              onClick={onCancel}
              className="flex-shrink-0 text-gray-500 hover:text-gray-700 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
          
          <div className="flex gap-3 justify-end">
            <button
              onClick={onCancel}
              className="px-6 py-2.5 bg-gray-200 hover:bg-gray-300 text-gray-800 font-medium rounded-lg transition-colors"
            >
              {cancelText}
            </button>
            <button
              onClick={onConfirm}
              className={`px-6 py-2.5 ${buttonColors[type]} text-white font-medium rounded-lg transition-colors`}
            >
              {confirmText}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

