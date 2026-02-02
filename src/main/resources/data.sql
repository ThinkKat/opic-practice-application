-- OPIc 연습 앱 초기 데이터
-- 생성일: 2026-02-01

INSERT INTO category (name) VALUES
('Housing'),
('Eating Out');

INSERT INTO question_type (name) VALUES
('Description'),
('Contrast'),
('Change over time'),
('Preference');

INSERT INTO session_status_mapping (status_code, status_text, description) VALUES
('SES0001', 'pending', '세션 생성됨, 시작 대기 중'),
('SES0002', 'in_progress', '세션 진행 중'),
('SES0003', 'paused', '세션 일시 중지됨'),
('SES0004', 'completed', '세션 완료됨'),
('SES0005', 'cancelled', '세션 취소됨');

INSERT INTO questions (category_id, question_type_id, question) VALUES
(1, 1, 'I would like to know about where you live. Describe your home in detail. What does it look like? How many rooms does it have? What is your favorite part of your home and why?'),
(1, 2, 'Compare the home you lived in when you were a child to the home you live in now. What are the main differences between those two houses? Please explain the similarities and differences in detail.'),
(1, 4, 'People have different preferences when it comes to furniture or home decor. What kind of furniture do you prefer to have in your house? Do you like modern, minimalist styles, or do you prefer more traditional and cozy items? Tell me why you prefer that style.'),
(2, 1, 'Tell me about a restaurant you frequently visit. Where is it located, and what kind of food do they serve? Describe the atmosphere of the place and why you like going there.'),
(2, 3, 'How have people''s dining habits in your country changed over the last few years? What are the differences between how people used to eat out in the past and how they do it now? Tell me about the changes you have noticed.'),
(2, 4, 'When you go out to eat, do you prefer going to a place you are familiar with, or do you like trying new restaurants that you''ve never been to before? Explain the reasons for your preference with some examples.');
