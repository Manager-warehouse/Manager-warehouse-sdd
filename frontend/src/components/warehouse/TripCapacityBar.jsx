import React from 'react';

const TripCapacityBar = ({ currentWeight, maxWeight }) => {
  const percentage = Math.min(Math.round((currentWeight / maxWeight) * 100), 100);
  
  let colorClass = 'bg-ink';
  if (percentage >= 90) colorClass = 'bg-red-500';
  else if (percentage >= 75) colorClass = 'bg-amber-500';

  return (
    <div className="w-full">
      <div className="flex justify-between text-xs mb-1">
        <span className="font-medium text-shade-60">Tải trọng: {currentWeight}kg</span>
        <span className="text-shade-40">Tối đa: {maxWeight}kg</span>
      </div>
      <div className="w-full bg-shade-30 rounded-full h-2 overflow-hidden">
        <div 
          className={`h-2 rounded-full transition-all duration-300 ${colorClass}`} 
          style={{ width: `${percentage}%` }}
        />
      </div>
      {percentage > 100 && (
        <p className="text-xs text-red-500 mt-1 font-medium">Vượt quá tải trọng cho phép!</p>
      )}
    </div>
  );
};

export default TripCapacityBar;
