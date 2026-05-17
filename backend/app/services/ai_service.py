import logging
from groq import Groq
from app.config import settings

logger = logging.getLogger(__name__)

class AIService:
    def __init__(self):
        self.api_key = settings.GROQ_API_KEY
        if self.api_key:
            self.client = Groq(api_key=self.api_key)
        else:
            self.client = None
            logger.warning("GROQ_API_KEY is not set. AI Service will operate in fallback mode with mock responses.")
        
        # Dual-Model Strategy:
        # 1. llama-3.1-8b-instant: Ultra-low latency, blazing fast, highly cost-effective (for rapid icebreaker generation)
        self.fast_model = "llama-3.1-8b-instant"
        
        # 2. llama-3.3-70b-versatile: Extremely high reasoning capability (for deep interest parsing & matching summaries)
        self.premium_model = "llama-3.3-70b-versatile"

    async def generate_icebreaker(
        self, 
        user_name: str, 
        user_interests: list, 
        peer_name: str, 
        peer_interests: list
    ) -> str:
        """
        Generates a quick, contextual, one-sentence icebreaker between two professionals 
        using the blazing fast llama-3.1-8b-instant model. 
        Highly optimized for real-time proximity meetings.
        """
        if not self.client:
            return self._generate_fallback_icebreaker(user_name, user_interests, peer_name, peer_interests)

        prompt = f"""
        You are an intelligent networking wingman assistant for the TapConnect mobile app.
        Two professionals just met via proximity scan:
        - User 1: {user_name} (Interests: {', '.join(user_interests) if user_interests else 'Networking, Business'})
        - User 2: {peer_name} (Interests: {', '.join(peer_interests) if peer_interests else 'Networking, Tech'})

        Generate a natural, friendly, one-sentence conversation starter (under 20 words) for {user_name} to initiate a chat with {peer_name}.
        Focus on shared or complementary interests. Do not include quotes or surrounding text. Keep it direct and casual.
        """

        try:
            # Using llama-3.1-8b-instant for maximum speed (blazing-fast response)
            chat_completion = self.client.chat.completions.create(
                messages=[
                    {
                        "role": "system",
                        "content": "You are a professional networking assistant. You write concise, casual, one-sentence conversation starters."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                model=self.fast_model,
                temperature=0.7,
                max_tokens=50
            )
            
            result = chat_completion.choices[0].message.content.strip()
            if result.startswith('"') and result.endswith('"'):
                result = result[1:-1]
            return result

        except Exception as e:
            logger.error(f"Error calling Groq fast_model: {e}. Falling back.")
            return self._generate_fallback_icebreaker(user_name, user_interests, peer_name, peer_interests)

    async def generate_profile_match_summary(
        self, 
        user_name: str, 
        user_role: str,
        user_org: str,
        user_interests: list, 
        peer_name: str, 
        peer_role: str,
        peer_org: str,
        peer_interests: list
    ) -> str:
        """
        Performs a deep interest alignment comparison and outputs a high-quality 
        professional match summary using the powerful llama-3.3-70b-versatile model.
        Perfect for rendering descriptive match context in connection cards.
        """
        if not self.client:
            return self._generate_fallback_summary(user_name, user_interests, peer_name, peer_interests)

        prompt = f"""
        You are a highly analytical professional recruiter and matchmaker for the TapConnect mobile networking app.
        Analyze the profiles of these two individuals who just connected:
        
        Professional 1:
        - Name: {user_name}
        - Role: {user_role or 'Professional'} @ {user_org or 'TapConnect User'}
        - Interests: {', '.join(user_interests) if user_interests else 'Professional Development'}

        Professional 2:
        - Name: {peer_name}
        - Role: {peer_role or 'Professional'} @ {peer_org or 'TapConnect User'}
        - Interests: {', '.join(peer_interests) if peer_interests else 'Professional Development'}

        Tasks:
        1. Identify any overlapping or complementary fields, technologies, roles, or skill sets.
        2. Generate a highly polished, professional summary (under 30 words) explaining exactly why they should network. For example: "You both share a passion for Compose and AI. Discuss Ahmed's Favorive tip and Sarah's recent startup project!"
        Keep the tone encouraging, premium, and concise. Do not include quotes or conversational preambles.
        """

        try:
            # Using the highly capable llama-3.3-70b-versatile for complex reasoning/summarization
            chat_completion = self.client.chat.completions.create(
                messages=[
                    {
                        "role": "system",
                        "content": "You are a professional networking strategist. You write concise, high-value profile matching summaries."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                model=self.premium_model,
                temperature=0.7,
                max_tokens=100
            )
            
            result = chat_completion.choices[0].message.content.strip()
            if result.startswith('"') and result.endswith('"'):
                result = result[1:-1]
            return result

        except Exception as e:
            logger.error(f"Error calling Groq premium_model: {e}. Falling back.")
            return self._generate_fallback_summary(user_name, user_interests, peer_name, peer_interests)

    def _generate_fallback_icebreaker(self, user_name: str, user_interests: list, peer_name: str, peer_interests: list) -> str:
        """Fallback mock icebreaker generator in case API key is missing or fails."""
        common = set(user_interests).intersection(set(peer_interests))
        if common:
            topic = list(common)[0]
            return f"Ask {peer_name} about their favorite project in {topic}."
        elif user_interests and peer_interests:
            return f"Ask {peer_name} how they got started in {peer_interests[0]}!"
        else:
            return f"Say hello to {peer_name} and ask them about their current professional goals!"

    def _generate_fallback_summary(self, user_name: str, user_interests: list, peer_name: str, peer_interests: list) -> str:
        """Fallback mock match summary generator in case API key is missing or fails."""
        common = set(user_interests).intersection(set(peer_interests))
        if common:
            topic = list(common)[0]
            return f"You both share an interest in {topic}. Ask {peer_name} about their favorite Compose tips or startup ideas!"
        else:
            return f"You connected! Share your professional goals and explore how you can collaborate on future projects."

ai_service = AIService()
