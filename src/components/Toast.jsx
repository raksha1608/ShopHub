// import { useEffect } from "react";
// import { CheckCircle, XCircle, AlertCircle, Info, X } from "lucide-react";
//
// export default function Toast({ message, type = "success", onClose, duration = 3000 }) {
//   useEffect(() => {
//     if (duration > 0) {
//       const timer = setTimeout(() => {
//         onClose();
//       }, duration);
//       return () => clearTimeout(timer);
//     }
//   }, [duration, onClose]);
//
//   const icons = {
//     success: <CheckCircle className="w-6 h-6 text-green-500" />,
//     error: <XCircle className="w-6 h-6 text-red-500" />,
//     warning: <AlertCircle className="w-6 h-6 text-yellow-500" />,
//     info: <Info className="w-6 h-6 text-blue-500" />,
//   };
//
//   const bgColors = {
//     success: "bg-green-50 border-green-200",
//     error: "bg-red-50 border-red-200",
//     warning: "bg-yellow-50 border-yellow-200",
//     info: "bg-blue-50 border-blue-200",
//   };
//
//   const textColors = {
//     success: "text-green-800",
//     error: "text-red-800",
//     warning: "text-yellow-800",
//     info: "text-blue-800",
//   };
//
//   return (
//     <div className="fixed inset-0 z-[9999] flex items-center justify-center pointer-events-none">
//       <div className="pointer-events-auto animate-fadeIn">
//         <div
//           className={`${bgColors[type]} border-2 rounded-lg shadow-2xl p-6 min-w-[400px] max-w-[500px] transform transition-all duration-300`}
//         >
//           <div className="flex items-start gap-4">
//             <div className="flex-shrink-0">{icons[type]}</div>
//             <div className="flex-1">
//               <p className={`${textColors[type]} font-medium text-lg leading-relaxed`}>
//                 {message}
//               </p>
//             </div>
//             <button
//               onClick={onClose}
//               className={`flex-shrink-0 ${textColors[type]} hover:opacity-70 transition-opacity`}
//             >
//               <X className="w-5 h-5" />
//             </button>
//           </div>
//         </div>
//       </div>
//     </div>
//   );
// }
//

import { useEffect } from "react";
import { CheckCircle, XCircle, AlertCircle, Info, X } from "lucide-react";

export default function Toast({ message, type = "success", onClose, duration = 3000 }) {
  useEffect(() => {
    if (duration > 0) {
      const timer = setTimeout(() => {
        onClose();
      }, duration);
      return () => clearTimeout(timer);
    }
  }, [duration, onClose]);

  const icons = {
    success: <CheckCircle className="w-6 h-6 text-green-500" />,
    error: <XCircle className="w-6 h-6 text-red-500" />,
    warning: <AlertCircle className="w-6 h-6 text-yellow-500" />,
    info: <Info className="w-6 h-6 text-blue-500" />,
  };

  const bgColors = {
    success: "bg-green-50 border-green-200",
    error: "bg-red-50 border-red-200",
    warning: "bg-yellow-50 border-yellow-200",
    info: "bg-blue-50 border-blue-200",
  };

  const textColors = {
    success: "text-green-800",
    error: "text-red-800",
    warning: "text-yellow-800",
    info: "text-blue-800",
  };

  return (
    <div className="fixed top-5 right-5 z-[9999] flex flex-col items-end space-y-3 pointer-events-none">
      <div className="pointer-events-auto animate-fadeIn">
        <div
          className={`${bgColors[type]} border-2 rounded-lg shadow-2xl p-5 min-w-[300px] max-w-[400px] transform transition-all duration-300`}
        >
          <div className="flex items-start gap-3">
            <div className="flex-shrink-0">{icons[type]}</div>
            <div className="flex-1">
              <p className={`${textColors[type]} font-medium text-lg leading-relaxed`}>
                {message}
              </p>
            </div>
            <button
              onClick={onClose}
              className={`flex-shrink-0 ${textColors[type]} hover:opacity-70 transition-opacity`}
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

